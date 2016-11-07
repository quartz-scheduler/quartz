package org.quartz.plugins.management;

import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.quartz.core.QuartzScheduler;
import org.quartz.impl.StdScheduler;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for classloader memory leak in the plugin
 */
public class ShutdownHookPluginTest {

    @Test
    public void testInitialize() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        when(scheduler.getSchedulerName()).thenReturn("Mock-Scheduler");

        ShutdownHookPlugin plugin = new ShutdownHookPlugin();
        plugin.initialize("TestName", scheduler, null);

        assertEquals(getShutdownHookCount(), 1);

        plugin.shutdown();

        assertEquals("After plugin shutdown the hook should not be present", getShutdownHookCount(), 0);
    }

    private int getShutdownHookCount() {
        try {
            Class clazz = Class.forName("java.lang.ApplicationShutdownHooks");
            Field field = clazz.getDeclaredField("hooks");
            field.setAccessible(true);
            Map<Thread, Thread> hooks = (Map<Thread, Thread>) field.get(null);

            return hooks.entrySet().size();
        } catch (Exception e) {
        }
        return 0;
    }
}