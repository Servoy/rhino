/** */
package org.eclipse.dltk.rhino.dbgp;

import java.util.Map;

final class StdOutCommand extends DBGPDebugger.Command {
    /** */
    private final DBGPDebugger debugger;

    /**
     * @param debugger
     */
    StdOutCommand(DBGPDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    void parseAndExecute(String command, Map<?, ?> options) {
        this.debugger.printResponse(
                "<response command=\"stdout\"\r\n"
                        + "          success=\"1\"\r\n"
                        + "          transaction_id=\""
                        + options.get("-i")
                        + "\">\r\n"
                        + "</response>\r\n"
                        + "");
    }
}
