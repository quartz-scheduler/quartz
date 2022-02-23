package org.quartz.impl.jdbcjobstore;

public class TriggerToAcquireDTO {

    private final String name;
    private final String group;
    private final boolean concurrentExecutionDisallowed;
    
    public TriggerToAcquireDTO(String name, String group, boolean concurrentExecutionDisallowed) {
        this.name = name;
        this.group = group;
        this.concurrentExecutionDisallowed = concurrentExecutionDisallowed;
    }
    
    public String getName() {
        return name;
    }
    
    public String getGroup() {
        return group;
    }
    
    public boolean getConcurrentExecutionDisallowed() {
        return concurrentExecutionDisallowed;
    }
}
