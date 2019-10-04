/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.impl.jdbcjobstore;

import java.util.TimeZone;

import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.ScheduleBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.impl.triggers.CalendarIntervalTriggerImpl;
import org.quartz.spi.OperableTrigger;

public class CalendarIntervalTriggerPersistenceDelegate extends SimplePropertiesTriggerPersistenceDelegateSupport {

    public boolean canHandleTriggerType(OperableTrigger trigger) {
        return ((trigger instanceof CalendarIntervalTriggerImpl) && !((CalendarIntervalTriggerImpl)trigger).hasAdditionalProperties());
    }

    public String getHandledTriggerTypeDiscriminator() {
        return TTYPE_CAL_INT;
    }

    @Override
    protected SimplePropertiesTriggerProperties getTriggerProperties(OperableTrigger trigger) {

        CalendarIntervalTriggerImpl calTrig = (CalendarIntervalTriggerImpl)trigger;
        
        SimplePropertiesTriggerProperties props = new SimplePropertiesTriggerProperties();
        
        props.setInt1(calTrig.getRepeatInterval());
        props.setString1(calTrig.getRepeatIntervalUnit().name());
        props.setInt2(calTrig.getTimesTriggered());
        props.setString2(calTrig.getTimeZone().getID());
        props.setBoolean1(calTrig.isPreserveHourOfDayAcrossDaylightSavings());
        props.setBoolean2(calTrig.isSkipDayIfHourDoesNotExist());
        
        return props;
    }

    @Override
    protected TriggerPropertyBundle getTriggerPropertyBundle(SimplePropertiesTriggerProperties props) {

        TimeZone tz = null; // if we use null, that's ok as system default tz will be used
        String tzId = props.getString2();
        if(tzId != null && tzId.trim().length() != 0) // there could be null entries from previously released versions
            tz = TimeZone.getTimeZone(tzId);
        
        ScheduleBuilder<?> sb = CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
            .withInterval(props.getInt1(), IntervalUnit.valueOf(props.getString1()))
            .inTimeZone(tz)
            .preserveHourOfDayAcrossDaylightSavings(props.isBoolean1())
            .skipDayIfHourDoesNotExist(props.isBoolean2());
        
        int timesTriggered = props.getInt2();
        
        String[] statePropertyNames = { "timesTriggered" };
        Object[] statePropertyValues = { timesTriggered };

        return new TriggerPropertyBundle(sb, statePropertyNames, statePropertyValues);
    }

}
