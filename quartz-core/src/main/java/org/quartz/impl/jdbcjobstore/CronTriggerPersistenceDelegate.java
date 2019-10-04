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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.OperableTrigger;

public class CronTriggerPersistenceDelegate implements TriggerPersistenceDelegate, StdJDBCConstants {

    protected String tablePrefix;
    protected String schedNameLiteral;

    public void initialize(String theTablePrefix, String schedName) {
        this.tablePrefix = theTablePrefix;
        this.schedNameLiteral = "'" + schedName + "'";
    }

    public String getHandledTriggerTypeDiscriminator() {
        return TTYPE_CRON;
    }

    public boolean canHandleTriggerType(OperableTrigger trigger) {
        return ((trigger instanceof CronTriggerImpl) && !((CronTriggerImpl)trigger).hasAdditionalProperties());
    }

    public int deleteExtendedTriggerProperties(Connection conn, TriggerKey triggerKey) throws SQLException {

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(Util.rtp(DELETE_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, triggerKey.getName());
            ps.setString(2, triggerKey.getGroup());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

    public int insertExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException {

        CronTrigger cronTrigger = (CronTrigger)trigger;
        
        PreparedStatement ps = null;
        
        try {
            ps = conn.prepareStatement(Util.rtp(INSERT_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, trigger.getKey().getName());
            ps.setString(2, trigger.getKey().getGroup());
            ps.setString(3, cronTrigger.getCronExpression());
            ps.setString(4, cronTrigger.getTimeZone().getID());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

    public TriggerPropertyBundle loadExtendedTriggerProperties(Connection conn, TriggerKey triggerKey) throws SQLException {

        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            ps = conn.prepareStatement(Util.rtp(SELECT_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, triggerKey.getName());
            ps.setString(2, triggerKey.getGroup());
            rs = ps.executeQuery();

            if (rs.next()) {
                String cronExpr = rs.getString(COL_CRON_EXPRESSION);
                String timeZoneId = rs.getString(COL_TIME_ZONE_ID);

                CronScheduleBuilder cb = CronScheduleBuilder.cronSchedule(cronExpr);
              
                if (timeZoneId != null) 
                    cb.inTimeZone(TimeZone.getTimeZone(timeZoneId));
                
                return new TriggerPropertyBundle(cb, null, null);
            }
            
            throw new IllegalStateException("No record found for selection of Trigger with key: '" + triggerKey + "' and statement: " + Util.rtp(SELECT_CRON_TRIGGER, tablePrefix, schedNameLiteral));
        } finally {
            Util.closeResultSet(rs);
            Util.closeStatement(ps);
        }
    }

    public int updateExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException {

        CronTrigger cronTrigger = (CronTrigger)trigger;
        
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(Util.rtp(UPDATE_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, cronTrigger.getCronExpression());
            ps.setString(2, cronTrigger.getTimeZone().getID());
            ps.setString(3, trigger.getKey().getName());
            ps.setString(4, trigger.getKey().getGroup());
            
            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

}
