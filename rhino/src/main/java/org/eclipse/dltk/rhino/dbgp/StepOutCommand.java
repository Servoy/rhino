/** */
package org.eclipse.dltk.rhino.dbgp;

import java.util.Map;

final class StepOutCommand extends DBGPDebugger.Command {
    /** */
    private final DBGPDebugger debugger;

    /**
     * @param debugger
     */
    StepOutCommand(DBGPDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    void parseAndExecute(String command, Map<?, ?> options) {
        Object tid = options.get("-i");
        this.debugger.setTransactionId((String) tid);
        DBGPStackManager stackManager = this.debugger.getStackManager();
        if (stackManager != null) stackManager.stepOut();
    }
}
