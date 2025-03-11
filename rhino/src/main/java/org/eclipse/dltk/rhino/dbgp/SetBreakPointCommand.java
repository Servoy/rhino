/** */
package org.eclipse.dltk.rhino.dbgp;

import java.util.Map;

final class SetBreakPointCommand extends DBGPDebugger.Command {
    /** */
    private final DBGPDebugger debugger;

    /**
     * @param debugger
     */
    SetBreakPointCommand(DBGPDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    void parseAndExecute(String command, Map<?, ?> options) {
        BreakPoint p = new BreakPoint(options);
        this.debugger.getBreakPointManager().addBreakPoint(p);
        this.debugger.printResponse(
                "<response command=\"breakpoint_set\"\r\n"
                        + " transaction_id=\""
                        + options.get("-i")
                        + "\" "
                        + " id=\"p"
                        + p.id
                        + "\" state=\"enabled\" > "
                        + "</response>\r\n"
                        + "");
    }
}
