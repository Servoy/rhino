/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.testutils.Utils;

class DestructuringCompileTest {

	@Test
	void arrayDestructuringParameterCompiles() {
		String code = "function f(obj){ return Object.entries(obj).filter(([key, value]) => value !== null); }";
		Utils.runWithAllModes(
				cx -> {
					cx.setLanguageVersion(Context.VERSION_ES6);
					assertDoesNotThrow(
							() -> cx.compileString(code, "test", 1, null));
					return null;
				});
	}

	@Test
	void arrayDestructuringParameterProducesCorrectResult() {
		Utils.assertWithAllModes_ES6(
				1,
				"function f(obj){ return Object.entries(obj).filter(([key, value]) => value !== null); }\n"
						+ "f({a: 1, b: null}).length;");
	}

	@Test
	void arrayDestructuringParameterExtractsValues() {
		Utils.assertWithAllModes_ES6(
				"a",
				"function f(obj){ return Object.entries(obj).filter(([key, value]) => value !== null); }\n"
						+ "f({a: 1, b: null})[0][0];");
	}

	@Test
	void objectDestructuringParameterCompiles() {
		String code = "var fn = ({key, value}) => key + '=' + value;";
		Utils.runWithAllModes(
				cx -> {
					cx.setLanguageVersion(Context.VERSION_ES6);
					assertDoesNotThrow(
							() -> cx.compileString(code, "test", 1, null));
					return null;
				});
	}

	@Test
	void objectDestructuringParameterProducesCorrectResult() {
		Utils.assertWithAllModes_ES6(
				"x=42",
				"var fn = ({key, value}) => key + '=' + value;\n"
						+ "fn({key: 'x', value: 42});");
	}

	@Test
	void standaloneArrayDestructuringDeclaration() {
		Utils.assertWithAllModes_ES6(
				2,
				"function f() { var [a, b] = [1, 2]; return b; }\n" + "f();");
	}

	@Test
	void standaloneObjectDestructuringDeclaration() {
		Utils.assertWithAllModes_ES6(
				"hello",
				"function f() { var {msg} = {msg: 'hello'}; return msg; }\n" + "f();");
	}

	@Test
	void standaloneDestructuringAssignment() {
		Utils.assertWithAllModes_ES6(
				3,
				"function f() { var a, b; [a, b] = [3, 4]; return a; }\n" + "f();");
	}

	@Test
	void blockScopedConstRemainsCorrect() {
		Utils.runWithMode(
				cx -> {
					cx.setLanguageVersion(Context.VERSION_ES6);
					Scriptable scope = cx.initStandardObjects();
					Object res = cx.evaluateString(
							scope,
							"function f() {\n"
									+ "  const outer = 'outer';\n"
									+ "  { const inner = 'inner'; var result = inner; }\n"
									+ "  return result;\n"
									+ "}\n"
									+ "f();",
							"test.js",
							1,
							null);
					assertEquals("inner", res);
					return null;
				},
				true);
	}

	@Test
	void blockScopedLetRemainsCorrect() {
		Utils.assertWithAllModes_ES6(
				"block",
				"function f() {\n"
						+ "  let x = 'outer';\n"
						+ "  { let x = 'block'; var result = x; }\n"
						+ "  return result;\n"
						+ "}\n"
						+ "f();");
	}

	@Test
	void constIsReadOnly() {
		Utils.assertWithAllModes_ES6(
				1,
				"function f() {\n"
						+ "  const x = 1;\n"
						+ "  try { x = 2; } catch(e) {}\n"
						+ "  return x;\n"
						+ "}\n"
						+ "f();");
	}

	@Test
	void destructuringInMapCallback() {
		Utils.assertWithAllModes_ES6(
				"a:1",
				"var entries = [['a', 1], ['b', 2]];\n"
						+ "entries.map(([k, v]) => k + ':' + v)[0];");
	}
}
