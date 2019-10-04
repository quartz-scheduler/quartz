package org.quartz.core.jmx;

import static javax.management.openmbean.SimpleType.DATE;
import static javax.management.openmbean.SimpleType.INTEGER;
import static javax.management.openmbean.SimpleType.STRING;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.spi.MutableTrigger;
import org.quartz.spi.OperableTrigger;

public class TriggerSupport {
    private static final String COMPOSITE_TYPE_NAME = "Trigger";
    private static final String COMPOSITE_TYPE_DESCRIPTION = "Trigger Details";
    private static final String[] ITEM_NAMES = new String[] { "name",
      "group", "jobName", "jobGroup", "description", "jobDataMap",
            "calendarName", "fireInstanceId", "misfireInstruction", "priority",
            "startTime", "endTime", "nextFireTime", "previousFireTime", "finalFireTime" };
    private static final String[] ITEM_DESCRIPTIONS = new String[] { "name",
            "group", "jobName", "jobGroup", "description", "jobDataMap",
            "calendarName", "fireInstanceId", "misfireInstruction", "priority",
      "startTime", "endTime", "nextFireTime", "previousFireTime", "finalFireTime" };
    private static final OpenType[] ITEM_TYPES = new OpenType[] { STRING,
            STRING, STRING, STRING, STRING, JobDataMapSupport.TABULAR_TYPE,
            STRING, STRING, INTEGER, INTEGER,
      DATE, DATE, DATE, DATE, DATE };
    private static final CompositeType COMPOSITE_TYPE;
    private static final String TABULAR_TYPE_NAME = "Trigger collection";
    private static final String TABULAR_TYPE_DESCRIPTION = "Trigger collection";
    private static final String[] INDEX_NAMES = new String[] { "name", "group" };
    private static final TabularType TABULAR_TYPE;

    static {
        try {
            COMPOSITE_TYPE = new CompositeType(COMPOSITE_TYPE_NAME,
                    COMPOSITE_TYPE_DESCRIPTION, ITEM_NAMES, ITEM_DESCRIPTIONS,
                    ITEM_TYPES);
            TABULAR_TYPE = new TabularType(TABULAR_TYPE_NAME,
                    TABULAR_TYPE_DESCRIPTION, COMPOSITE_TYPE, INDEX_NAMES);
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String[] getItemNames() {
        return ITEM_NAMES;
    }

    public static String[] getItemDescriptions() {
        return ITEM_DESCRIPTIONS;
    }
    
    public static OpenType[] getItemTypes() {
        return ITEM_TYPES;
    }
    
    public String[] getIndexNames() {
        return INDEX_NAMES;
    }
    
    public static CompositeData toCompositeData(Trigger trigger) {
        try {
            return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES,
                    new Object[] {
                            trigger.getKey().getName(),
                            trigger.getKey().getGroup(),
                            trigger.getJobKey().getName(),
                            trigger.getJobKey().getGroup(),
                            trigger.getDescription(),
                            JobDataMapSupport.toTabularData(trigger
                                    .getJobDataMap()),
                            trigger.getCalendarName(),
                            ((OperableTrigger)trigger).getFireInstanceId(),
                            trigger.getMisfireInstruction(),
                            trigger.getPriority(), trigger.getStartTime(),
                            trigger.getEndTime(), trigger.getNextFireTime(),
                            trigger.getPreviousFireTime(),
                            trigger.getFinalFireTime() });
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public static TabularData toTabularData(List<? extends Trigger> triggers) {
        TabularData tData = new TabularDataSupport(TABULAR_TYPE);
        if (triggers != null) {
            ArrayList<CompositeData> list = new ArrayList<CompositeData>();
            for (Trigger trigger : triggers) {
                list.add(toCompositeData(trigger));
            }
            tData.putAll(list.toArray(new CompositeData[list.size()]));
        }
        return tData;
    }
    
    public static List<CompositeData> toCompositeList(List<? extends Trigger> triggers) {
        List<CompositeData> result = new ArrayList<CompositeData>();
        for(Trigger trigger : triggers) {
            CompositeData cData = TriggerSupport.toCompositeData(trigger);
            if(cData != null) {
                result.add(cData);
            }
        }
        return result;
    }
    
    public static void initializeTrigger(MutableTrigger trigger, CompositeData cData) {
        trigger.setDescription((String) cData.get("description"));
        trigger.setCalendarName((String) cData.get("calendarName"));
        if(cData.containsKey("priority")) {
            trigger.setPriority(((Integer)cData.get("priority")).intValue());
        }
        if(cData.containsKey("jobDataMap")) {
            trigger.setJobDataMap(JobDataMapSupport.newJobDataMap((TabularData)cData.get("jobDataMap")));
        }
        Date startTime;
        if(cData.containsKey("startTime")) {
            startTime = (Date) cData.get("startTime");
        } else {
            startTime = new Date();
        }
        trigger.setStartTime(startTime);
        trigger.setEndTime((Date) cData.get("endTime"));
        if(cData.containsKey("misfireInstruction")) {
            trigger.setMisfireInstruction(((Integer)cData.get("misfireInstruction")).intValue());
        }
        trigger.setKey(new TriggerKey((String) cData.get("name"), (String) cData.get("group")));
        trigger.setJobKey(new JobKey((String) cData.get("jobName"), (String) cData.get("jobGroup")));
    }
    
    public static void initializeTrigger(MutableTrigger trigger, Map<String, Object> attrMap) {
        trigger.setDescription((String) attrMap.get("description"));
        trigger.setCalendarName((String) attrMap.get("calendarName"));
        if(attrMap.containsKey("priority")) {
            trigger.setPriority(((Integer)attrMap.get("priority")).intValue());
        }
        if(attrMap.containsKey("jobDataMap")) {
            @SuppressWarnings("unchecked") // cast as expected.
            Map<String, Object> mapTyped = (Map<String, Object>)attrMap.get("jobDataMap");
            trigger.setJobDataMap(JobDataMapSupport.newJobDataMap(mapTyped));
        }
        Date startTime;
        if(attrMap.containsKey("startTime")) {
            startTime = (Date) attrMap.get("startTime");
        } else {
            startTime = new Date();
        }
        trigger.setStartTime(startTime);
        if(attrMap.containsKey("endTime")) {
            trigger.setEndTime((Date) attrMap.get("endTime"));
        }
        if(attrMap.containsKey("misfireInstruction")) {
            trigger.setMisfireInstruction(((Integer)attrMap.get("misfireInstruction")).intValue());
        }
        trigger.setKey(new TriggerKey((String) attrMap.get("name"), (String) attrMap.get("group")));
        trigger.setJobKey(new JobKey((String) attrMap.get("jobName"), (String) attrMap.get("jobGroup")));
    }
    
    public static OperableTrigger newTrigger(CompositeData cData) throws ParseException {
        OperableTrigger result = null;
        if(cData.containsKey("cronExpression")) {
            result = CronTriggerSupport.newTrigger(cData);
        } else {
            result = SimpleTriggerSupport.newTrigger(cData);
        }
        return result;
    }
    
    public static OperableTrigger newTrigger(Map<String, Object> attrMap) throws ParseException {
        OperableTrigger result = null;
        if(attrMap.containsKey("cronExpression")) {
            result = CronTriggerSupport.newTrigger(attrMap);
        } else {
            result = SimpleTriggerSupport.newTrigger(attrMap);
        }
        return result;
    }
    
}
