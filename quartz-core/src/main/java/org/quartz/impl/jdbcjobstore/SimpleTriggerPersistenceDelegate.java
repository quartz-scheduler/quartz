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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.OperableTrigger;

public class SimpleTriggerPersistenceDelegate implements TriggerPersistenceDelegate, StdJDBCConstants {

    protected String tablePrefix;
    protected String schedNameLiteral;

    public void initialize(String theTablePrefix, String schedName) {
        this.tablePrefix = theTablePrefix;
        this.schedNameLiteral = "'" + schedName + "'";
    }

    public String getHandledTriggerTypeDiscriminator() {
        return TTYPE_SIMPLE;
    }

    public boolean canHandleTriggerType(OperableTrigger trigger) {
        return ((trigger instanceof SimpleTriggerImpl) && !((SimpleTriggerImpl)trigger).hasAdditionalProperties());
    }

    public int deleteExtendedTriggerProperties(Connection conn, TriggerKey triggerKey) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(Util.rtp(DELETE_SIMPLE_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, triggerKey.getName());
            ps.setString(2, triggerKey.getGroup());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

    public int insertExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException {

        SimpleTrigger simpleTrigger = (SimpleTrigger)trigger;
        
        PreparedStatement ps = null;
        
        try {
            ps = conn.prepareStatement(Util.rtp(INSERT_SIMPLE_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, trigger.getKey().getName());
            ps.setString(2, trigger.getKey().getGroup());
            ps.setInt(3, simpleTrigger.getRepeatCount());
            ps.setBigDecimal(4, new BigDecimal(String.valueOf(simpleTrigger.getRepeatInterval())));
            ps.setInt(5, simpleTrigger.getTimesTriggered());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

    public TriggerPropertyBundle loadExtendedTriggerProperties(Connection conn, TriggerKey triggerKey) throws SQLException {

        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            ps = conn.prepareStatement(Util.rtp(SELECT_SIMPLE_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, triggerKey.getName());
            ps.setString(2, triggerKey.getGroup());
            rs = ps.executeQuery();
    
            if (rs.next()) {
                int repeatCount = rs.getInt(COL_REPEAT_COUNT);
                long repeatInterval = rs.getLong(COL_REPEAT_INTERVAL);
                int timesTriggered = rs.getInt(COL_TIMES_TRIGGERED);

                SimpleScheduleBuilder sb = SimpleScheduleBuilder.simpleSchedule()
                    .withRepeatCount(repeatCount)
                    .withIntervalInMilliseconds(repeatInterval);
                
                String[] statePropertyNames = { "timesTriggered" };
                Object[] statePropertyValues = { timesTriggered };
                
                return new TriggerPropertyBundle(sb, statePropertyNames, statePropertyValues);
            }
            
            throw new IllegalStateException("No record found for selection of Trigger with key: '" + triggerKey + "' and statement: " + Util.rtp(SELECT_SIMPLE_TRIGGER, tablePrefix, schedNameLiteral));
        } finally {
            Util.closeResultSet(rs);
            Util.closeStatement(ps);
        }
    }

    public int updateExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException {

        SimpleTrigger simpleTrigger = (SimpleTrigger)trigger;
        
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(Util.rtp(UPDATE_SIMPLE_TRIGGER, tablePrefix, schedNameLiteral));

            ps.setInt(1, simpleTrigger.getRepeatCount());
            ps.setBigDecimal(2, new BigDecimal(String.valueOf(simpleTrigger.getRepeatInterval())));
            ps.setInt(3, simpleTrigger.getTimesTriggered());
            ps.setString(4, simpleTrigger.getKey().getName());
            ps.setString(5, simpleTrigger.getKey().getGroup());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

}
