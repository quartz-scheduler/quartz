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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;

/**
 * An interface which provides an implementation for storing a particular
 * type of <code>Trigger</code>'s extended properties.
 *  
 * @author jhouse
 */
public interface TriggerPersistenceDelegate {

    public void initialize(String tablePrefix, String schedulerName);
    
    public boolean canHandleTriggerType(OperableTrigger trigger);
    
    public String getHandledTriggerTypeDiscriminator();
    
    public int insertExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException;

    public int updateExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException;
    
    public int deleteExtendedTriggerProperties(Connection conn, TriggerKey triggerKey) throws SQLException;

    public TriggerPropertyBundle loadExtendedTriggerProperties(Connection conn, TriggerKey triggerKey) throws SQLException;
    
    
    class TriggerPropertyBundle {
        
        private ScheduleBuilder<?> sb;
        private String[] statePropertyNames;
        private Object[] statePropertyValues;
        
        public TriggerPropertyBundle(ScheduleBuilder<?> sb, String[] statePropertyNames, Object[] statePropertyValues) {
            this.sb = sb;
            this.statePropertyNames = statePropertyNames;
            this.statePropertyValues = statePropertyValues;
        }

        public ScheduleBuilder<?> getScheduleBuilder() {
            return sb;
        }

        public String[] getStatePropertyNames() {
            return statePropertyNames;
        }

        public Object[] getStatePropertyValues() {
            return statePropertyValues;
        }
    }
}
