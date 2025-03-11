/** */
package org.eclipse.dltk.rhino.dbgp;

import java.util.Map;

final class RemoveBreakPointCommand extends DBGPDebugger.Command {
    /** */
    private final DBGPDebugger debugger;

    /**
     * @param debugger
     */
    RemoveBreakPointCommand(DBGPDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    void parseAndExecute(String command, Map<?, ?> options) {
        this.debugger.getBreakPointManager().removeBreakPoint((String) options.get("-d"));
        this.debugger.printResponse(
                "<response command=\"breakpoint_remove\"\r\n"
                        + " transaction_id=\""
                        + options.get("-i")
                        + "\" />");
    }
}
