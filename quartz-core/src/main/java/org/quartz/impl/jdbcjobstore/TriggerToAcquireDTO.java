package org.quartz.impl.jdbcjobstore;

import org.quartz.spi.OperableTrigger;

public class TriggerToAcquireDTO {

    private final OperableTrigger trigger;
    private final boolean concurrentExecutionDisallowed;

    public TriggerToAcquireDTO(OperableTrigger trigger, boolean concurrentExecutionDisallowed) {
        this.trigger = trigger;
        this.concurrentExecutionDisallowed = concurrentExecutionDisallowed;
    }

    public OperableTrigger getTrigger() {
        return trigger;
    }

    public boolean getConcurrentExecutionDisallowed() {
        return concurrentExecutionDisallowed;
    }

}
