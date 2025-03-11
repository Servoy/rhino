/** */
package org.eclipse.dltk.rhino.dbgp;

import java.util.Map;

final class StepOverCommand extends DBGPDebugger.Command {
    /** */
    private final DBGPDebugger debugger;

    /**
     * @param debugger
     */
    StepOverCommand(DBGPDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    void parseAndExecute(String command, Map<?, ?> options) {
        Object tid = options.get("-i");
        this.debugger.setTransactionId((String) tid);
        DBGPStackManager stackManager = this.debugger.getStackManager();
        if (stackManager != null && stackManager.getStackDepth() > 0) {
            stackManager.stepOver();
        } else {
            synchronized (this.debugger) {
                while (!this.debugger.isInited) {
                    try {
                        Thread.sleep(10); // Sleep for 10 milliseconds
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restore interrupted status
                    }
                }
                this.debugger.notify();
            }
            stackManager = this.debugger.getStackManager();
            if (stackManager != null) stackManager.resume();
        }
    }
}
