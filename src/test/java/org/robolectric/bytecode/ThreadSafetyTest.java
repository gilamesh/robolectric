package org.robolectric.bytecode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricConfig;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.TestConfigs;
import org.robolectric.internal.Implementation;
import org.robolectric.internal.Implements;
import org.robolectric.internal.Instrument;
import org.robolectric.internal.RealObject;

import java.lang.reflect.Field;

import static org.junit.Assert.assertSame;
import static org.robolectric.Robolectric.*;

@RunWith(RobolectricTestRunner.class) @RobolectricConfig(TestConfigs.WithoutDefaults.class)
public class ThreadSafetyTest {
    @Test
    public void shadowCreationShouldBeThreadsafe() throws Exception {
        getShadowWrangler().bindShadowClass(InstrumentedThreadShadow.class);
        Field field = InstrumentedThread.class.getDeclaredField("shadowFromOtherThread");
        field.setAccessible(true);

        for (int i = 0; i < 100; i++) { // :-(
            InstrumentedThread instrumentedThread = new InstrumentedThread();
            instrumentedThread.start();
            Object shadowFromThisThread = shadowOf_(instrumentedThread);

            instrumentedThread.join();
            Object shadowFromOtherThread = field.get(instrumentedThread);
            assertSame(shadowFromThisThread, shadowFromOtherThread);
        }
    }

    @Instrument
    public static class InstrumentedThread extends Thread {
        InstrumentedThreadShadow shadowFromOtherThread;

        @Override
        public void run() {
            shadowFromOtherThread = shadowOf_(this);
        }
    }

    @Implements(InstrumentedThread.class)
    public static class InstrumentedThreadShadow {
        @RealObject InstrumentedThread realObject;
        @Implementation
        public void run() {
            directlyOn(realObject).run();
        }
    }
}