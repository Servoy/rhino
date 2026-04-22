package org.eclipse.dltk.rhino.dbgp;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeWith;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

public class DBGPDebugFrame implements DebugFrame {

	private final Deque<NativeWith> nativeWith = new java.util.ArrayDeque<>();

    private final String sourceName;
    private final DBGPStackManager stackManager;
    private int lineNumber;
    private String where;
    private Scriptable thisObj;
    private Scriptable scope;
    private DebuggableScript script;
    private boolean suspend;
    private boolean callOnEnter;
    

    public boolean isSuspend() {
        return suspend;
    }

    public void setSuspend(boolean suspend) {
        this.suspend = suspend;
    }

    public DBGPDebugFrame(Context ct, DebuggableScript node, DBGPDebugger debugger) {
        sourceName = node.getSourceName();
        stackManager = DBGPStackManager.getManager(ct, debugger);
        where = node.getFunctionName();
        this.script = node;
        if (where == null) {
            where = "module";
        }
    }

	public Map<String,Object> getParametersAndVars() {
		Map<String,Object> paramsAndVars = new HashMap<>();
		for (int a = 0; a < script.getParamAndVarCount(); a++) {
			String name = script.getParamOrVarName(a);
			paramsAndVars.put(name, scope.get(name, thisObj));
		}
		DebuggableScript ds = this.script;
		Scriptable sc = scope;
		while (!ds.isTopLevel() && ds.getParent() != null) {
			DebuggableScript parent = ds.getParent();
			sc = sc.getParentScope();
			while (sc instanceof NativeWith) {
				for (Object id : sc.getIds()) {
					paramsAndVars.put(id.toString(), sc.get(id.toString(),sc));
				}
				sc = sc.getParentScope();
			}
			for (int a = 0; a < parent.getParamAndVarCount(); a++) {
				String name = parent.getParamOrVarName(a);
				paramsAndVars.put(name, sc.get(name, sc));
			}
			ds = parent;
		}
		if (nativeWith.size() > 0) {
			Scriptable withScope = nativeWith.peek();
			while (withScope instanceof NativeWith) {
				for (Object id : withScope.getIds()) {
					paramsAndVars.put(id.toString(), withScope.get(id.toString(),withScope));
				}
				withScope = withScope.getParentScope();
			}
		}
		return paramsAndVars;
	}

    @Override
    public void onDebuggerStatement(Context cx) {
        // TODO TEST
        System.err.println("ONDEBUGGER STATEMENT " + cx);
    }

    @Override
    public void onEnter(Context cx, Scriptable activation, Scriptable thisObj, Object[] args) {
        this.scope = activation;

        this.thisObj = thisObj;
        callOnEnter = true;
    }

    @Override
    public void onExceptionThrown(Context cx, Throwable ex) {
        stackManager.exceptionThrown(ex);
    }

    @Override
    public void onExit(Context cx, boolean byThrow, Object resultOrException) {

        stackManager.exit(this);
    }

    @Override
    public void onLineChange(Context cx, int lineNumber) {
        this.lineNumber = lineNumber;
        if (callOnEnter) {
            callOnEnter = false;
            stackManager.enter(this);
        } else {
            stackManager.changeLine(this, lineNumber);
        }
    }
    
    @Override
    public void onNativeWithEnter(Context cx, NativeWith withScope) {
    	nativeWith.push(withScope);
    	
    }
    
    @Override
    public void onNativeWithExit(Context cx, NativeWith withScope) {
    	NativeWith pop = nativeWith.pop();
    	if (pop != withScope) {
			throw new IllegalStateException("Popped scope is different from exited one");
		}
    }

    public String getSourceName() {
        return sourceName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * @return fully qualified name of js method e.g. /path/to/file.js.method. Used for getting the
     *     BreakPoint from the BreakPointManager see {@link
     *     org.eclipse.dltk.rhino.dbgp.BreakPoint#getFullyQualifiedName()}
     */
    public String getWhere() {
        return sourceName + "." + where;
    }

    public Object getStackFrameArgs() {
        Object object = scope.get("arguments", thisObj);
        return object;
    }

    public Scriptable getThis() {
        return thisObj;
    }

    public void setValue(String name, String value) {
        if (name.startsWith("this.")) {
            name = name.substring("this.".length());
            thisObj.put(name, thisObj, eval(value));
        } else scope.put(name, scope, value);
    }

    public Object eval(String value) {
        boolean contextCreated = false;
        Context context = Context.getCurrentContext();
        if (context == null) {
            contextCreated = true;
            context = Context.enter();
        }
        Debugger debugger = context.getDebugger();

        try {
            context.setDebugger(null, null);
            Scriptable cs = scope;
            if (nativeWith.size() > 0) {
				cs = nativeWith.peek();
			}
            if (value.startsWith("this.")) {

                value = value.substring("this.".length());
                cs = thisObj;
            } else if (value.equals("this")) return thisObj;
            Object evaluateString = context.evaluateString(cs, value, "eval", 0, null);
            return evaluateString;

        } catch (Throwable e) {
            return "Error during evaluation:" + e.getMessage();
        } finally {

            context.setDebugger(debugger, null);

            if (contextCreated) Context.exit();
        }
    }

    public Object getValue(String longName) {
        if (longName.equals("this")) return thisObj;
        if (longName.startsWith("this.")) {
            longName = longName.substring("this.".length());
            return getProperty(thisObj, longName);
        }
        return getProperty(scope, longName);
    }

    private Object getProperty(Scriptable obj, String longName) {
        int k = longName.indexOf('.');
        if (k == -1) return shortGet(obj, longName);
        String shortName = longName.substring(0, k);
        String sm = longName.substring(k + 1);
        Object property = shortGet(obj, shortName);
        if (property instanceof Scriptable) {
            return getProperty((Scriptable) property, sm);
        }
        return null;
    }

    private Object shortGet(Scriptable obj, String longName) {
        if (obj instanceof NativeJavaArray || obj instanceof NativeArray) {
            longName = longName.trim();
            if (longName.startsWith("[")) longName = longName.substring(1, longName.length() - 1);
            int parseInt = Integer.parseInt(longName);
            return obj.get(parseInt, obj);
        }
        Scriptable parent = obj;
        while (parent != null) {
            Object o = ScriptableObject.getProperty(parent, longName);
            if (o != null && o != Scriptable.NOT_FOUND) return o;
            parent = parent.getParentScope();
        }
        try {
            int parseInt = Integer.parseInt(longName);
            Object o = ScriptableObject.getProperty(obj, parseInt);
            if (o != null && o != Scriptable.NOT_FOUND) return o;
        } catch (Exception e) {
        }
        return null;
    }
}
