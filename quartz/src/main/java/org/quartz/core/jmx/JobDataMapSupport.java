package org.quartz.core.jmx;

import static javax.management.openmbean.SimpleType.STRING;

import java.util.ArrayList;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.quartz.JobDataMap;

public class JobDataMapSupport {
    private static final String TYPE_NAME = "JobDataMap";
    private static final String[] keyValue = new String[] { "key", "value" };
    private static final OpenType[] openTypes = new OpenType[] { STRING, STRING };
    private static final CompositeType rowType;
    public static final TabularType TABULAR_TYPE;

    static {
        try {
            rowType = new CompositeType(TYPE_NAME, TYPE_NAME, keyValue, keyValue,
                    openTypes);
            TABULAR_TYPE = new TabularType(TYPE_NAME, TYPE_NAME, rowType,
                    new String[] { "key" });
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public static JobDataMap newJobDataMap(TabularData tabularData) {
        JobDataMap jobDataMap = new JobDataMap();

        if(tabularData != null) {
            for (Object o : tabularData.values()) {
                CompositeData cData = (CompositeData) o;
                jobDataMap.put((String) cData.get("key"), (String) cData.get("value"));
            }
        }
        
        return jobDataMap;
    }

    public static JobDataMap newJobDataMap(Map<String, Object> map) {
        JobDataMap jobDataMap = new JobDataMap();

        if (map != null) {
            jobDataMap.putAll(map);
        }

        return jobDataMap;
    }
    
    /**
     * @return composite data
     */
    public static CompositeData toCompositeData(String key, String value) {
        try {
            return new CompositeDataSupport(rowType, keyValue, new Object[] {
                    key, value });
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param jobDataMap
     * @return TabularData
     */
    public static TabularData toTabularData(JobDataMap jobDataMap) {
        TabularData tData = new TabularDataSupport(TABULAR_TYPE);
        ArrayList<CompositeData> list = new ArrayList<>();
        for (Map.Entry<String, Object> entry : jobDataMap.entrySet()) {
            list.add(toCompositeData(entry.getKey(), String.valueOf(entry.getValue())));
        }
        tData.putAll(list.toArray(new CompositeData[list.size()]));
        return tData;
    }

}
