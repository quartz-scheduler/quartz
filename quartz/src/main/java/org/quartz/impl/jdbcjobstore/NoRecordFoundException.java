package org.quartz.impl.jdbcjobstore;

import org.quartz.TriggerKey;

public class NoRecordFoundException extends IllegalStateException {

    private static final long serialVersionUID = 1L;
    private final TriggerKey triggerKey;
    private final String scheduleName;
    private final String statement;
    private final Class<?> delegateClass;

    public NoRecordFoundException(TriggerKey triggerKey, String scheduleName, Class<?> delegateClass) {
        super("No record found for selection of Trigger with key: '" + triggerKey + "' and Scheduler: " + scheduleName
        + " and Delegate: " + delegateClass);
        this.triggerKey = triggerKey;
        this.scheduleName = scheduleName;
        this.delegateClass = delegateClass;
        this.statement = null;
    }

    public NoRecordFoundException(TriggerKey triggerKey, String scheduleName, String statement) {
        super("No record found for selection of Trigger with key: '" + triggerKey + "' and statement: " + statement);
        this.triggerKey = triggerKey;
        this.scheduleName = scheduleName;
        this.statement = statement;
        this.delegateClass = null;
    }
}
