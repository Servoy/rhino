/** */
package org.eclipse.dltk.rhino.dbgp;

import java.util.Map;

final class UpdateBreakPointCommand extends DBGPDebugger.Command {
    /** */
    private final DBGPDebugger debugger;

    /**
     * @param debugger
     */
    UpdateBreakPointCommand(DBGPDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    void parseAndExecute(String command, Map<?, ?> options) {

        String id = (String) options.get("-d");
        String newState = (String) options.get("-s");
        String newLine = (String) options.get("-n");
        String hitValue = (String) options.get("-h");
        String hitCondition = (String) options.get("-o");
        String condEString = (String) options.get("--");

        if (condEString != null) {
            condEString = Base64Helper.decodeString(condEString);
        }

        try {
            this.debugger
                    .getBreakPointManager()
                    .updateBreakpoint(id, newState, newLine, hitValue, hitCondition, condEString);
        } catch (Exception e) {
            System.err.println("Error updating breakpoint" + e);
        }
        this.debugger.printResponse(
                "<response command=\"breakpoint_update\"\r\n"
                        + " transaction_id=\""
                        + options.get("-i")
                        + "\">\r\n"
                        + " id=\""
                        + id
                        + "\" state=\""
                        + newState
                        + "\" "
                        + "</response>\r\n"
                        + "");
    }
}
