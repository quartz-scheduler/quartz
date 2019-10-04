package org.quartz.core.jmx;

import static javax.management.openmbean.SimpleType.BOOLEAN;
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

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.impl.JobDetailImpl;

public class JobDetailSupport {
    private static final String COMPOSITE_TYPE_NAME = "JobDetail";
    private static final String COMPOSITE_TYPE_DESCRIPTION = "Job Execution Details";
    private static final String[] ITEM_NAMES = new String[] { "name", "group",
            "description", "jobClass", "jobDataMap", "durability", "shouldRecover",};
    private static final String[] ITEM_DESCRIPTIONS = new String[] { "name",
            "group", "description", "jobClass", "jobDataMap", "durability", "shouldRecover",};
    private static final OpenType[] ITEM_TYPES = new OpenType[] { STRING,
            STRING, STRING, STRING, JobDataMapSupport.TABULAR_TYPE, BOOLEAN,
            BOOLEAN, };
    private static final CompositeType COMPOSITE_TYPE;
    private static final String TABULAR_TYPE_NAME = "JobDetail collection";
    private static final String TABULAR_TYPE_DESCRIPTION = "JobDetail collection";
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

    /**
     * @param cData
     * @return JobDetail
     */
    public static JobDetail newJobDetail(CompositeData cData)
      throws ClassNotFoundException
    {
        JobDetailImpl jobDetail = new JobDetailImpl();

        int i = 0;
        jobDetail.setName((String) cData.get(ITEM_NAMES[i++]));
        jobDetail.setGroup((String) cData.get(ITEM_NAMES[i++]));
        jobDetail.setDescription((String) cData.get(ITEM_NAMES[i++]));
        Class<?> jobClass = Class.forName((String) cData.get(ITEM_NAMES[i++]));
        @SuppressWarnings("unchecked")
        Class<? extends Job> jobClassTyped = (Class<? extends Job>)jobClass;
        jobDetail.setJobClass(jobClassTyped);
        jobDetail.setJobDataMap(JobDataMapSupport.newJobDataMap((TabularData) cData.get(ITEM_NAMES[i++])));
        jobDetail.setDurability((Boolean) cData.get(ITEM_NAMES[i++]));
        jobDetail.setRequestsRecovery((Boolean) cData.get(ITEM_NAMES[i++]));

        return jobDetail;
    }

    /**
     * @param attrMap the attributes that define the job
     * @return JobDetail
     */
    public static JobDetail newJobDetail(Map<String, Object> attrMap)
        throws ClassNotFoundException
    {
        JobDetailImpl jobDetail = new JobDetailImpl();

        int i = 0;
        jobDetail.setName((String) attrMap.get(ITEM_NAMES[i++]));
        jobDetail.setGroup((String) attrMap.get(ITEM_NAMES[i++]));
        jobDetail.setDescription((String) attrMap.get(ITEM_NAMES[i++]));
        Class<?> jobClass = Class.forName((String) attrMap.get(ITEM_NAMES[i++]));
        @SuppressWarnings("unchecked")
        Class<? extends Job> jobClassTyped = (Class<? extends Job>)jobClass;
        jobDetail.setJobClass(jobClassTyped);
        if(attrMap.containsKey(ITEM_NAMES[i])) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)attrMap.get(ITEM_NAMES[i]); 
            jobDetail.setJobDataMap(JobDataMapSupport.newJobDataMap(map));
        }
        i++;
        if(attrMap.containsKey(ITEM_NAMES[i])) {
            jobDetail.setDurability((Boolean) attrMap.get(ITEM_NAMES[i]));
        }
        i++;
        if(attrMap.containsKey(ITEM_NAMES[i])) {
            jobDetail.setRequestsRecovery((Boolean) attrMap.get(ITEM_NAMES[i]));
        }
        i++;
        
        return jobDetail;
    }
    
    /**
     * @param jobDetail
     * @return CompositeData
     */
    public static CompositeData toCompositeData(JobDetail jobDetail) {
        try {
            return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES,
                    new Object[] {
                            jobDetail.getKey().getName(),
                            jobDetail.getKey().getGroup(),
                            jobDetail.getDescription(),
                            jobDetail.getJobClass().getName(),
                            JobDataMapSupport.toTabularData(jobDetail
                                    .getJobDataMap()), 
                            jobDetail.isDurable(),
                            jobDetail.requestsRecovery(), });
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public static TabularData toTabularData(JobDetail[] jobDetails) {
        TabularData tData = new TabularDataSupport(TABULAR_TYPE);
        if (jobDetails != null) {
            ArrayList<CompositeData> list = new ArrayList<CompositeData>();
            for (JobDetail jobDetail : jobDetails) {
                list.add(toCompositeData(jobDetail));
            }
            tData.putAll(list.toArray(new CompositeData[list.size()]));
        }
        return tData;
    }

}
