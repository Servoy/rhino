# Spec: SVY-21250 — Array-destructuring parameter fails to compile (NullPointerException in `visitLet`)

## 1. Goal

Compiling a function or arrow whose parameter is a destructuring pattern — e.g.
`Object.entries(obj).filter(([key, value]) => value !== null)` — currently throws a
raw `java.lang.NullPointerException: Cannot read field "next" because "this.first" is null`
from `NodeTransformer.visitLet`. Upstream `org.mozilla:rhino:1.9.1` compiles the same
code correctly, so this is a regression introduced by a Servoy fork patch. The goal is to
make destructuring parameters (and destructuring assignments in general) compile again,
while preserving the block-scoped `const` behaviour that the offending patch added.

## 2. Background

### 2.1 The reported failure

Reproduction script from the ticket:

```javascript
var Context = org.mozilla.javascript.Context;
var code = 'function f(obj){ return Object.entries(obj).filter(([key, value]) => value !== null); }';
var cx = Context.enter();
try {
  cx.setLanguageVersion(Context.VERSION_ES6);
  cx.compileString(code, 'repro', 1, null);
  print('OK - compiled');
} catch(e){ print('FAIL - ' + e); }
finally { Context.exit(); }
```

The actual exception (from the SVY-21250 comment by Johan Compagner) is:

```
java.lang.NullPointerException: Cannot read field "next" because "this.first" is null
    at org.mozilla.javascript.Node.removeChild(Node.java:293)
    at org.mozilla.javascript.NodeTransformer.visitLet(NodeTransformer.java:459)
    at org.mozilla.javascript.NodeTransformer.transformCompilationUnit_r(NodeTransformer.java:278)
    ...
    at org.mozilla.javascript.CodeGenerator.compile(CodeGenerator.java:68)
    at org.mozilla.javascript.Interpreter.compile(Interpreter.java:712)
    at org.mozilla.javascript.Context.compileImpl(Context.java:2609)
    at org.mozilla.javascript.Context.compileString(Context.java:1537)
    ...
```

The comment thread makes clear this was initially closed as a DLTK/editor issue
("added support for destructuring parameters, but the type inferencer is not fully
correct yet"), but Johan correctly re-identified it as a **runtime Rhino defect** that is
*not* fixed for 26.9 and needs investigation in the Rhino code itself.

### 2.2 Git history — link to SVY-19261

`git blame` on `NodeTransformer.java` lines 444–459 attributes the new child-splitting
logic (lines 446–457) to commit **`e5387d851e`**:

```
e5387d851e  Johan Compagner  2025-11-21 17:20:05 +0100
    SVY-19261 Invalid scoping of const variable
```

SVY-19261 ("Invalid scoping of const variable", closed, fixVersion 2026.3.0) added a
`FEATURE_LEGACY_CONST_MODE` feature flag so that `const` is scoped like `let` (but
read-only), fixing [mozilla/rhino#326](https://github.com/mozilla/rhino/issues/326).
As part of that change, `NodeTransformer` was reworked in two connected places:

1. **`transformCompilationUnit_r` (lines 99–114)** — when a block `Scope` with a symbol
   table is turned into a synthetic `let`, it now builds a node with **two** grouping
   children: an `innerLet` (`Token.LET`) and an `innerConst` (`Token.CONST`), followed by
   the original body:

   ```java
   Node let = new Node(type == Token.ARRAYCOMP ? Token.LETEXPR : Token.LET);
   Node innerLet = new Node(Token.LET);
   Node innerConst = new Node(Token.CONST);   // always added, even when empty
   let.addChildToBack(innerLet);
   let.addChildToBack(innerConst);
   ...
   let.addChildToBack(oldNode);               // the body
   ```

2. **`visitLet` (lines 444–459)** — to consume that new 3-child layout, the code was
   changed from the original single line:

   ```java
   Node vars = scopeNode.getFirstChild();
   Node body = vars.getNext();
   ```

   to:

   ```java
   Node vars = scopeNode.getFirstChild();
   Node consts = null;
   Node body = null;
   if (!(vars instanceof VariableDeclaration)) {   // <-- faulty discriminator
       consts = vars.getNext();
       body   = consts.getNext();
       scopeNode.removeChild(consts);
   } else {
       body = vars.getNext();
   }
   scopeNode.removeChild(vars);
   scopeNode.removeChild(body);                     // line 459 — NPE here
   ```

The fix must not revert SVY-19261; the block-scoped `const` behaviour and its jsunit
test (added by Marius Muntean and validated on SVY-19261) must keep working. No separate
`docs/` spec exists for SVY-19261 (checked — `docs/` has no `.spec.md` files).

### 2.3 Root cause

`visitLet` is reached from the `LETEXPR`/`LET` case of `transformCompilationUnit_r`
(lines 268–280) whenever a scope node's first child is a `Token.LET` node. There are
**three** distinct shapes of node that arrive here:

| Case | Origin | Children of `scopeNode` |
|------|--------|-------------------------|
| A. Legacy `let (…) {}` statement/expression | `IRFactory.transformLetNode` | `VariableDeclaration` (vars), body |
| B. Block-scoped `let`/`const` | `transformCompilationUnit_r` lines 99–114 | `innerLet` (`Token.LET`), `innerConst` (`Token.CONST`), body |
| C. **Destructuring assignment / parameter** | `Parser.destructuringAssignmentHelper` (creates a `LETEXPR` scope) | `Node(Token.LET, tempName)`, `COMMA` (body) |

The new discriminator `!(vars instanceof VariableDeclaration)` is `true` for **both B and
C**, because in case C the first child is a plain `Node` of type `Token.LET` (not a
`VariableDeclaration`). So case C is wrongly routed into the "has consts" branch:

- `vars` = the `Token.LET(tempName)` node
- `consts` = `vars.getNext()` = the `COMMA` body node
- `body` = `consts.getNext()` = **`null`** (the `COMMA` is the last child)

Then `scopeNode.removeChild(consts)` removes the `COMMA`, `scopeNode.removeChild(vars)`
removes the `LET`, leaving `scopeNode` with no children (`first == null`). Finally
`scopeNode.removeChild(body)` with `body == null` calls `getChildBefore(null)` which
returns `null` (since `first == null`), then executes `first = first.next` →
**NPE on `this.first`**. This matches the stack trace exactly (Node.java:293 via
NodeTransformer.java:459).

Upstream Rhino has only cases A and C and used the single `body = vars.getNext()` line,
which is why upstream compiles the sample correctly.

## 3. Design

### 3.1 Correctly discriminate the three node shapes

Case B is uniquely identifiable: `transformCompilationUnit_r` **always** appends the
`innerConst` node (`Token.CONST`) as the second child, even when there are no `const`
symbols. Cases A and C never have a `Token.CONST` node in that position (case A's second
child is the body; case C's second child is a `Token.COMMA` body).

Therefore `visitLet`'s child-splitting should key off the presence of a `Token.CONST`
grouping node as the second child, rather than off `vars instanceof VariableDeclaration`.

Proposed replacement for lines 445–459:

```java
Node vars = scopeNode.getFirstChild();
Node consts = null;
Node body;
// Block-scoped let/const (see transformCompilationUnit_r) produces a
// separate CONST grouping node as the second child. Legacy let statements
// and destructuring LETEXPRs do not, and must fall back to the original
// "body is the second child" behaviour.
Node second = vars.getNext();
if (second != null && second.getType() == Token.CONST) {
    consts = second;
    body = consts.getNext();
    scopeNode.removeChild(consts);
} else {
    body = second;
}
scopeNode.removeChild(vars);
scopeNode.removeChild(body);
```

This keeps case B (the SVY-19261 behaviour) intact — the `innerConst` node is still split
out and later fed to `fillObjectLiterals(..., /*forConst=*/true)` — and restores the
original upstream behaviour for cases A and C, where `body = vars.getNext()`.

`import ...ast.VariableDeclaration;` becomes unused in `visitLet`; leave the import only
if still referenced elsewhere (it is not — `organizeImports` will remove it). Verify with
Organize Imports / spotless after the change.

### 3.2 Why not fix it in the parser instead

The destructuring LETEXPR shape (case C) is long-standing upstream behaviour and is
consumed correctly by the rest of `visitLet` (the `for (Node v = vars.getFirstChild()…)`
loop and the `if (consts != null)` guard already handle a `null` `consts`). The minimal,
lowest-risk fix is entirely local to the child-splitting block that SVY-19261 introduced.
Changing the parser would risk regressing the block-scoping work.

## 4. Implementation plan

1. **`rhino/src/main/java/org/mozilla/javascript/NodeTransformer.java`** — in `visitLet`
   (currently lines 444–459), replace the `!(vars instanceof VariableDeclaration)`
   discriminator with the `second != null && second.getType() == Token.CONST` check
   described in §3.1.
2. Remove the now-unused `import org.mozilla.javascript.ast.VariableDeclaration;` if no
   other reference remains (run Organize Imports).
3. Run `./gradlew spotlessApply` to reformat.
4. **Add a JUnit 5 regression test** (new test class, per AGENTS.md). Place it in the
   `tests` module alongside other compile/eval tests, or in `rhino` if a pure
   `Context.compileString` test fits better there. The test must, at minimum:
   - compile `function f(obj){ return Object.entries(obj).filter(([key, value]) => value !== null); }`
     under `VERSION_ES6` in **interpreted mode** (the failing path is
     `Interpreter.compile` → `CodeGenerator.compile`) and assert no exception is thrown;
   - additionally cover an **object**-destructuring parameter
     (`({key, value}) => …`) and a standalone destructuring assignment
     (`var [a, b] = arr;`) inside a function;
   - evaluate the destructuring arrow to assert it also *runs* correctly, e.g.
     `Object.entries({a:1, b:null}).filter(([k, v]) => v !== null)` yields the `a` entry.
5. **Add a block-scoped `const` guard test** (or confirm SVY-19261's existing jsunit test
   still passes) to prove case B did not regress — e.g. the SVY-19261 sample:
   ```javascript
   function invalidScope() {
     var out = [];
     for (let i = 1; i < 4; i++) { const c = i; out.push(c); }
     return out; // [1, 2, 3]
   }
   ```
6. Run `./gradlew check` (tests + spotless) and confirm green.
7. **Bundle version:** ask the user whether `Bundle-Version` in `META-INF/MANIFEST.MF`
   (currently `1.9.1.s2`) should be bumped to `1.9.1.s3`. Per AGENTS.md the qualifier is
   incremented once per Servoy release cycle; a bump to `.s2` already happened on this
   branch, so a further code change may or may not warrant `.s3` depending on whether the
   `.s2` bump belongs to the current release cycle.

## 5. Acceptance criteria

- [ ] `function f(obj){ return Object.entries(obj).filter(([key, value]) => value !== null); }`
      compiles under `VERSION_ES6` without throwing (interpreted mode).
- [ ] Object-destructuring parameters (`({key, value}) => …`) compile and run correctly.
- [ ] Standalone destructuring assignments/declarations continue to compile and run.
- [ ] The destructuring arrow produces the correct runtime result (filter keeps non-null
      entries).
- [ ] Block-scoped `const`/`let` scoping from SVY-19261 still behaves correctly
      (const is block-scoped and read-only); existing SVY-19261 test still passes.
- [ ] `./gradlew check` passes (tests + spotless formatting).

## 6. Out of scope

- The DLTK / script-editor type-inferencer inaccuracy for destructuring parameters noted
  in Edit Mera's comment (that is an editor-side concern, tracked separately).
- Any broader rework of destructuring support beyond fixing the compile regression.
- Changes to the `FEATURE_LEGACY_CONST_MODE` semantics introduced by SVY-19261.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `Bundle-Version` be bumped to `1.9.1.s3`, or does the existing `.s2` bump already cover the current release cycle? | Johan Compagner | open |
| Preferred home for the new regression test — `rhino` (pure `Context.compileString`) vs `tests` (jsunit `.js` under `testsrc/jstests`)? | dev | open |
