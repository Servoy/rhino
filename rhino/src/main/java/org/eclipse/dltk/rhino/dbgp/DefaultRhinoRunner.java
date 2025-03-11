package org.eclipse.dltk.rhino.dbgp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class DefaultRhinoRunner {

    public void run(String[] args) {
        Context cx = Context.enter();
        if (args.length > 1) {
            String host = args[1];
            String porg = args[2];
            DBGPDebugger debugger;
            try {
                final Socket s = new Socket(host, Integer.parseInt(porg));
                debugger = new DBGPDebugger(s, args[0], args[3], cx);

                debugger.start();
                cx.setDebugger(debugger, null);

                Scriptable scope = cx.initStandardObjects();
                extraInit(scope, cx);
                synchronized (debugger) {
                    try {
                        debugger.isInited = true;
                        debugger.wait();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException();
                    }
                }
                try {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        System.err.println("Interrupted " + e.getMessage());
                    }
                    cx.setGeneratingDebug(true);
                    cx.setInterpretedMode(true);
                    cx.evaluateReader(
                            scope,
                            new FileReader(args[0], StandardCharsets.UTF_8),
                            new File(args[0]).getAbsolutePath(),
                            0,
                            null);

                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    System.err.println("File not found " + e.getMessage());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    System.err.println("IO Error " + e.getMessage());
                }
                debugger.notifyEnd();
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number " + e.getMessage());
            } catch (UnknownHostException e) {
                System.err.println("Unknown host " + e.getMessage());
            } catch (IOException e) {
                System.err.println("IO Error " + e.getMessage());
            }
        } else {
            Scriptable scope = cx.initStandardObjects();
            try {
                cx.evaluateReader(
                        scope, new FileReader(args[0], StandardCharsets.UTF_8), args[0], 0, null);
            } catch (FileNotFoundException e) {
                System.err.println("File not found " + e.getMessage());
            } catch (IOException e) {
                System.err.println("IO Error " + e.getMessage());
            }
        }
    }

    protected void extraInit(Scriptable scope, Context cx) {}
}
