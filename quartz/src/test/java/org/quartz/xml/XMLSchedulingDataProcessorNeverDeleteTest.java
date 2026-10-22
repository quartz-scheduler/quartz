package org.quartz.xml;

import org.junit.jupiter.api.Test;
import org.quartz.simpl.CascadingClassLoadHelper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the *ToNeverDelete accessors return the correct backing list.
 *
 * @see <a href="https://github.com/quartz-scheduler/quartz/issues/1487">Issue #1487</a>
 */
public class XMLSchedulingDataProcessorNeverDeleteTest {
    @Test
    public void getJobGroupsToNeverDeleteReturnsAddedGroup() throws Exception {
        XMLSchedulingDataProcessor processor = newProcessor();

        processor.addJobGroupToNeverDelete("group1");
        assertTrue(processor.getJobGroupsToNeverDelete().contains("group1"));
        assertFalse(processor.getJobGroupsToNeverDelete().isEmpty());
    }

    @Test
    public void getTriggerGroupsToNeverDeleteReturnsAddedGroup() throws Exception {
        XMLSchedulingDataProcessor processor = newProcessor();

        processor.addTriggerGroupToNeverDelete("tgroup1");
        assertTrue(processor.getTriggerGroupsToNeverDelete().contains("tgroup1"));
        assertFalse(processor.getTriggerGroupsToNeverDelete().isEmpty());
    }

    private static XMLSchedulingDataProcessor newProcessor() throws Exception {
        CascadingClassLoadHelper clhelper = new CascadingClassLoadHelper();
        clhelper.initialize();
        return new XMLSchedulingDataProcessor(clhelper);
    }
}
