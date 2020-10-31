package com.oreo.paint;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.oreo.paint.help.KeyValManager;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.oreo.paint", appContext.getPackageName());

        KeyValManager.setContext(appContext);
        KeyValManager.put("Test Key", true);
        KeyValManager.put("Test Key", "str");
        KeyValManager.put("Test Key", 10);
        KeyValManager.put("Test Key", (float) 10.1);
        KeyValManager.put("Test Key", 10L);
    }
}