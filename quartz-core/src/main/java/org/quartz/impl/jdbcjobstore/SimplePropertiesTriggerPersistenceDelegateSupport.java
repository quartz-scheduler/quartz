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

import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;

/**
 * A base implementation of {@link TriggerPersistenceDelegate} that persists 
 * trigger fields in the "QRTZ_SIMPROP_TRIGGERS" table.  This allows extending
 * concrete classes to simply implement a couple methods that do the work of
 * getting/setting the trigger's fields, and creating the {@link ScheduleBuilder}
 * for the particular type of trigger. 
 * 
 * @see CalendarIntervalTriggerPersistenceDelegate for an example extension
 * 
 * @author jhouse
 */
public abstract class SimplePropertiesTriggerPersistenceDelegateSupport implements TriggerPersistenceDelegate, StdJDBCConstants {

    protected static final String TABLE_SIMPLE_PROPERTIES_TRIGGERS = "SIMPROP_TRIGGERS";
    
    protected static final String COL_STR_PROP_1 = "STR_PROP_1";
    protected static final String COL_STR_PROP_2 = "STR_PROP_2";
    protected static final String COL_STR_PROP_3 = "STR_PROP_3";
    protected static final String COL_INT_PROP_1 = "INT_PROP_1";
    protected static final String COL_INT_PROP_2 = "INT_PROP_2";
    protected static final String COL_LONG_PROP_1 = "LONG_PROP_1";
    protected static final String COL_LONG_PROP_2 = "LONG_PROP_2";
    protected static final String COL_DEC_PROP_1 = "DEC_PROP_1";
    protected static final String COL_DEC_PROP_2 = "DEC_PROP_2";
    protected static final String COL_BOOL_PROP_1 = "BOOL_PROP_1";
    protected static final String COL_BOOL_PROP_2 = "BOOL_PROP_2";
    
    protected static final String SELECT_SIMPLE_PROPS_TRIGGER = "SELECT *" + " FROM "
        + TABLE_PREFIX_SUBST + TABLE_SIMPLE_PROPERTIES_TRIGGERS + " WHERE "
        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
        + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    protected static final String DELETE_SIMPLE_PROPS_TRIGGER = "DELETE FROM "
        + TABLE_PREFIX_SUBST + TABLE_SIMPLE_PROPERTIES_TRIGGERS + " WHERE "
        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
        + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    protected static final String INSERT_SIMPLE_PROPS_TRIGGER = "INSERT INTO "
        + TABLE_PREFIX_SUBST + TABLE_SIMPLE_PROPERTIES_TRIGGERS + " ("
        + COL_SCHEDULER_NAME + ", "
        + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + ", "
        + COL_STR_PROP_1 + ", " + COL_STR_PROP_2 + ", " + COL_STR_PROP_3 + ", "
        + COL_INT_PROP_1 + ", " + COL_INT_PROP_2 + ", "
        + COL_LONG_PROP_1 + ", " + COL_LONG_PROP_2 + ", "
        + COL_DEC_PROP_1 + ", " + COL_DEC_PROP_2 + ", "
        + COL_BOOL_PROP_1 + ", " + COL_BOOL_PROP_2 
        + ") " + " VALUES(" + SCHED_NAME_SUBST + ", ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    protected static final String UPDATE_SIMPLE_PROPS_TRIGGER = "UPDATE "
        + TABLE_PREFIX_SUBST + TABLE_SIMPLE_PROPERTIES_TRIGGERS + " SET "
        + COL_STR_PROP_1 + " = ?, " + COL_STR_PROP_2 + " = ?, " + COL_STR_PROP_3 + " = ?, "
        + COL_INT_PROP_1 + " = ?, " + COL_INT_PROP_2 + " = ?, "
        + COL_LONG_PROP_1 + " = ?, " + COL_LONG_PROP_2 + " = ?, "
        + COL_DEC_PROP_1 + " = ?, " + COL_DEC_PROP_2 + " = ?, "
        + COL_BOOL_PROP_1 + " = ?, " + COL_BOOL_PROP_2 
        + " = ? WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
        + " AND " + COL_TRIGGER_NAME
        + " = ? AND " + COL_TRIGGER_GROUP + " = ?";
    
    protected String tablePrefix;

    protected String schedNameLiteral;

    public void initialize(String theTablePrefix, String schedName) {
        this.tablePrefix = theTablePrefix;
        this.schedNameLiteral = "'" + schedName + "'";
    }

    protected abstract SimplePropertiesTriggerProperties getTriggerProperties(OperableTrigger trigger);
    
    protected abstract TriggerPropertyBundle getTriggerPropertyBundle(SimplePropertiesTriggerProperties properties);
    
    public int deleteExtendedTriggerProperties(Connection conn, TriggerKey triggerKey) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(Util.rtp(DELETE_SIMPLE_PROPS_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, triggerKey.getName());
            ps.setString(2, triggerKey.getGroup());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

    public int insertExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException {

        SimplePropertiesTriggerProperties properties = getTriggerProperties(trigger);
        
        PreparedStatement ps = null;
        
        try {
            ps = conn.prepareStatement(Util.rtp(INSERT_SIMPLE_PROPS_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, trigger.getKey().getName());
            ps.setString(2, trigger.getKey().getGroup());
            ps.setString(3, properties.getString1());
            ps.setString(4, properties.getString2());
            ps.setString(5, properties.getString3());
            ps.setInt(6, properties.getInt1());
            ps.setInt(7, properties.getInt2());
            ps.setLong(8, properties.getLong1());
            ps.setLong(9, properties.getLong2());
            ps.setBigDecimal(10, properties.getDecimal1());
            ps.setBigDecimal(11, properties.getDecimal2());
            ps.setBoolean(12, properties.isBoolean1());
            ps.setBoolean(13, properties.isBoolean2());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

    public TriggerPropertyBundle loadExtendedTriggerProperties(Connection conn, TriggerKey triggerKey) throws SQLException {

        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            ps = conn.prepareStatement(Util.rtp(SELECT_SIMPLE_PROPS_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, triggerKey.getName());
            ps.setString(2, triggerKey.getGroup());
            rs = ps.executeQuery();
    
            if (rs.next()) {
                SimplePropertiesTriggerProperties properties = new SimplePropertiesTriggerProperties();
                    
                properties.setString1(rs.getString(COL_STR_PROP_1));
                properties.setString2(rs.getString(COL_STR_PROP_2));
                properties.setString3(rs.getString(COL_STR_PROP_3));
                properties.setInt1(rs.getInt(COL_INT_PROP_1));
                properties.setInt2(rs.getInt(COL_INT_PROP_2));
                properties.setLong1(rs.getInt(COL_LONG_PROP_1));
                properties.setLong2(rs.getInt(COL_LONG_PROP_2));
                properties.setDecimal1(rs.getBigDecimal(COL_DEC_PROP_1));
                properties.setDecimal2(rs.getBigDecimal(COL_DEC_PROP_2));
                properties.setBoolean1(rs.getBoolean(COL_BOOL_PROP_1));
                properties.setBoolean2(rs.getBoolean(COL_BOOL_PROP_2));
                
                return getTriggerPropertyBundle(properties);
            }
            
            throw new IllegalStateException("No record found for selection of Trigger with key: '" + triggerKey + "' and statement: " + Util.rtp(SELECT_SIMPLE_TRIGGER, tablePrefix, schedNameLiteral));
        } finally {
            Util.closeResultSet(rs);
            Util.closeStatement(ps);
        }
    }

    public int updateExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException {

        SimplePropertiesTriggerProperties properties = getTriggerProperties(trigger);
        
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(Util.rtp(UPDATE_SIMPLE_PROPS_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, properties.getString1());
            ps.setString(2, properties.getString2());
            ps.setString(3, properties.getString3());
            ps.setInt(4, properties.getInt1());
            ps.setInt(5, properties.getInt2());
            ps.setLong(6, properties.getLong1());
            ps.setLong(7, properties.getLong2());
            ps.setBigDecimal(8, properties.getDecimal1());
            ps.setBigDecimal(9, properties.getDecimal2());
            ps.setBoolean(10, properties.isBoolean1());
            ps.setBoolean(11, properties.isBoolean2());
            ps.setString(12, trigger.getKey().getName());
            ps.setString(13, trigger.getKey().getGroup());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

}
