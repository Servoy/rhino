/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.junit.Test;

/**
 * SVY-21337 regression test for {@code MemberBox.searchAccessibleMethod}.
 *
 * <p>When a public method's declaring class lives in a non-exported JPMS package (e.g. {@code
 * sun.nio.ch.FileChannelImpl}), a direct reflective invoke throws {@link
 * InaccessibleObjectException}/{@link IllegalAccessException}. Rhino must recover by rerouting to
 * the same method on a public supertype/interface (e.g. {@code java.nio.channels.FileChannel.read}).
 *
 * <p>The rhino 1.9.0 update re-activated a guard {@code if (!Modifier.isPublic(declaringClass))}
 * around that reroute. {@code FileChannelImpl} is itself public, so the guard skipped the reroute,
 * leaving the invoke to fail and breaking svyIO.zip()/channelCopy (0-byte files). Removing the
 * guard restored the 2026.3 behavior.
 *
 * <p>This test drives the (private static) {@code searchAccessibleMethod} directly via reflection
 * so it genuinely fails if the guard is ever re-introduced. A plain end-to-end script cannot catch
 * the regression because {@code JavaMembers} member discovery often resolves to an accessible
 * overload before {@code searchAccessibleMethod} is reached.
 */
public class NonExportedModuleMethodTest {

    /**
     * Sanity precondition: the runtime channel is the public-but-non-exported {@code
     * sun.nio.ch.FileChannelImpl}, and invoking its {@code read(ByteBuffer)} Method directly fails
     * with an access error. If this ever stops holding, the regression scenario no longer applies.
     */
    @Test
    public void preconditionDirectInvokeIsInaccessible() throws Exception {
        File f = createTempFileWithContent();
        try (FileInputStream fis = new FileInputStream(f)) {
            FileChannel ch = fis.getChannel();
            Class<?> runtimeClass = ch.getClass();
            assertEquals(
                    "runtime channel must be the non-exported impl class",
                    "sun.nio.ch.FileChannelImpl",
                    runtimeClass.getName());
            assertTrue(
                    "impl class is public (that is precisely why the old guard skipped the reroute)",
                    Modifier.isPublic(runtimeClass.getModifiers()));

            Method implRead = runtimeClass.getMethod("read", ByteBuffer.class);
            boolean threw = false;
            try {
                implRead.invoke(ch, ByteBuffer.allocate(16));
            } catch (ReflectiveOperationException | InaccessibleObjectException ex) {
                threw = true;
            }
            assertTrue("direct invoke of FileChannelImpl.read must fail with an access error", threw);
            ch.close();
        } finally {
            f.delete();
        }
    }

    /**
     * Core regression check: {@code MemberBox.searchAccessibleMethod} must reroute {@code
     * FileChannelImpl.read(ByteBuffer)} to the public {@code FileChannel.read(ByteBuffer)}, and the
     * rerouted method must be invocable without an access error. Returns null (and the test fails)
     * if the public-declaring-class guard is re-introduced.
     */
    @Test
    public void searchAccessibleMethodReroutesNonExportedPublicClass() throws Exception {
        File f = createTempFileWithContent();
        try (FileInputStream fis = new FileInputStream(f)) {
            FileChannel ch = fis.getChannel();
            Method implRead = ch.getClass().getMethod("read", ByteBuffer.class);

            Class<?> memberBox = Class.forName("org.mozilla.javascript.MemberBox");
            Method search =
                    memberBox.getDeclaredMethod(
                            "searchAccessibleMethod", Method.class, Class[].class);
            search.setAccessible(true);

            Method rerouted =
                    (Method)
                            search.invoke(
                                    null, implRead, new Class<?>[] {ByteBuffer.class});

            assertNotNull(
                    "searchAccessibleMethod must reroute to a public supertype/interface method"
                            + " for a public-but-non-exported declaring class (SVY-21337)",
                    rerouted);
            assertTrue(
                    "rerouted method must be declared on a public class",
                    Modifier.isPublic(rerouted.getDeclaringClass().getModifiers()));
            assertTrue(
                    "rerouted declaring class must be a public supertype/interface of the channel",
                    rerouted.getDeclaringClass().isInstance(ch));

            // The rerouted method must actually be invocable (no access error).
            Object bytesRead = rerouted.invoke(ch, ByteBuffer.allocate(1024));
            assertEquals(5, ((Number) bytesRead).intValue());
            ch.close();
        } finally {
            f.delete();
        }
    }

    private static File createTempFileWithContent() throws Exception {
        File f = File.createTempFile("rhino-svy21337", ".txt");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write("hello".getBytes());
        }
        return f;
    }
}
