# Rhino

Rhino is an open source (MPL 2.0) JavaScript engine, implemented in Java 11. Upstream, the build system used is gradle,
via the gradle wrapper (`./gradlew`). There are no external dependencies, except for JUnit for unit tests.

**Servoy fork note:** In this Servoy fork the project is built with **Maven (`mvn`)**, not gradle, and it is packaged as
an Eclipse/OSGi bundle (see `META-INF/MANIFEST.MF`). The gradle build may fail here because Servoy code uses Java 16+
features (records, pattern-matching `instanceof`) while the upstream gradle build enforces `options.release = 11`.
Prefer Maven for building, and run/verify tests through the **Eclipse JUnit test runner** (Run As > JUnit Test, or the
`eclipse-ide` / `eclipse-pde` JUnit tools) rather than `./gradlew test`.

## Useful commands

1. Build (Servoy): `mvn` (Maven) — this is how Servoy builds the project.
2. Run tests (Servoy): use the **Eclipse JUnit test runner** (e.g. the `eclipse-ide_runClassTests` /
   `eclipse-ide_runAllTests` tools, or Run As > JUnit Test in the IDE).
3. Upstream gradle commands (may not work in this fork, kept for reference):
   - Build: `./gradlew build`
   - Run tests: `./gradlew test`
   - Format code: `./gradlew spotlessApply`
   - Checks (tests, formatting): `./gradlew check`

## Rules and code style

- Important: try to follow the existing code style and patterns.
- Be restrained in regard to writing code comments.
- Always add unit tests for any new feature or bug fixes. They should go either in `rhino` or `tests`; search for
  existing tests to make a decision on a case-by-case basis.
- New test classes should be written using JUnit 5. Migrating existing tests from JUnit 4 to JUnit 5 is not a goal
  though, unless explicitly requested.
- Code style is enforced via spotless. After every change, reformat the code.
- **Bundle versioning:** When making changes, ask the user whether the `Bundle-Version` in `META-INF/MANIFEST.MF` should be incremented. The version bump happens once per Servoy release cycle (e.g., once for 2026.6, once for 2026.9). If no bump has occurred yet on the current branch for the current release and there are code changes, the qualifier should be incremented (e.g., `1.9.1.s1` -> `1.9.1.s2`).

## Code organization

The code base is organized in multiple modules. Most changes will go into the `rhino` or `tests` modules. Refer to
README.md for the full list.

- `rhino`: The primary codebase necessary and sufficient to run JavaScript code
- `rhino-tools`: Contains the shell, debugger, and the "Global" object, which many tests and other Rhino-based tools use
- `tests`: The tests that depend on all of Rhino and also the external tests, including the Mozilla legacy test scripts
  and the test262 tests

## Architecture

Rhino follows a classical architecture:

- [TokenStream](rhino/src/main/java/org/mozilla/javascript/TokenStream.java) is the lexer
- [Parser](rhino/src/main/java/org/mozilla/javascript/Parser.java) is the parser, which produces an AST modeled by the
  subclasses of [AstNode](rhino/src/main/java/org/mozilla/javascript/ast/AstNode.java)
- the [IRFactory](rhino/src/main/java/org/mozilla/javascript/IRFactory.java) will generate a tree IR, modeled
  by [Node](rhino/src/main/java/org/mozilla/javascript/Node.java)
- there are two backends:
    - one which generates java classes, in [Codegen](rhino/src/main/java/org/mozilla/javascript/optimizer/Codegen.java);
    - and one which generates a bytecode for [Interpreter](rhino/src/main/java/org/mozilla/javascript/Interpreter.java),
      in [CodeGenerator](rhino/src/main/java/org/mozilla/javascript/CodeGenerator.java).

[ScriptRuntime](rhino/src/main/java/org/mozilla/javascript/ScriptRuntime.java) is the main class for all runtime
methods, shared between compiled classes and interpreter.

Builtins such as `Object` or `Array` are implemented in
[NativeObject](rhino/src/main/java/org/mozilla/javascript/NativeObject.java),
[NativeArray](rhino/src/main/java/org/mozilla/javascript/NativeArray.java), etc.
