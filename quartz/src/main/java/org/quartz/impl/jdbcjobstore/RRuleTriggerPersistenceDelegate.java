/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
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

import org.quartz.JobDetail;
import org.quartz.RRuleScheduleBuilder;
import org.quartz.RRuleTrigger;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.RRuleTriggerImpl;
import org.quartz.spi.OperableTrigger;

/**
 * Persists the extended properties of an {@link RRuleTrigger} (its RFC&nbsp;5545
 * <code>RRULE</code> string and time zone) to the <code>QRTZ_RRULE_TRIGGERS</code>
 * table. This is the RRULE analogue of {@link CronTriggerPersistenceDelegate}.
 */
public class RRuleTriggerPersistenceDelegate implements TriggerPersistenceDelegate, StdJDBCConstants {

    protected String tablePrefix;
    protected String schedNameLiteral;

    public void initialize(String theTablePrefix, String schedName) {
        this.tablePrefix = theTablePrefix;
        this.schedNameLiteral = "'" + schedName + "'";
    }

    public String getHandledTriggerTypeDiscriminator() {
        return TTYPE_RRULE;
    }

    public boolean canHandleTriggerType(OperableTrigger trigger) {
        return ((trigger instanceof RRuleTriggerImpl) && !((RRuleTriggerImpl) trigger).hasAdditionalProperties());
    }

    public int deleteExtendedTriggerProperties(Connection conn, TriggerKey triggerKey) throws SQLException {

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(Util.rtp(DELETE_RRULE_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, triggerKey.getName());
            ps.setString(2, triggerKey.getGroup());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

    public int insertExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state,
            JobDetail jobDetail) throws SQLException, IOException {

        RRuleTrigger rruleTrigger = (RRuleTrigger) trigger;

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(Util.rtp(INSERT_RRULE_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, trigger.getKey().getName());
            ps.setString(2, trigger.getKey().getGroup());
            ps.setString(3, rruleTrigger.getRRuleExpression());
            ps.setString(4, rruleTrigger.getTimeZone().getID());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

    public TriggerPropertyBundle loadExtendedTriggerProperties(Connection conn, TriggerKey triggerKey)
            throws SQLException {

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(Util.rtp(SELECT_RRULE_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, triggerKey.getName());
            ps.setString(2, triggerKey.getGroup());
            rs = ps.executeQuery();
            if (rs.next()) {
                return loadExtendedTriggerPropertiesFromResultSet(rs, triggerKey);
            }

            throw new NoRecordFoundException(triggerKey, schedNameLiteral,
                    Util.rtp(SELECT_RRULE_TRIGGER, tablePrefix, schedNameLiteral));

        } finally {
            Util.closeResultSet(rs);
            Util.closeStatement(ps);
        }
    }

    public TriggerPropertyBundle loadExtendedTriggerPropertiesFromResultSet(ResultSet rs, TriggerKey triggerKey)
            throws SQLException {
        if (Util.areNull(rs, COL_RRULE_EXPRESSION, COL_TIME_ZONE_ID)) {
            throw new NoRecordFoundException(triggerKey, schedNameLiteral, this.getClass());
        }
        String rruleExpr = rs.getString(COL_RRULE_EXPRESSION);
        String timeZoneId = rs.getString(COL_TIME_ZONE_ID);

        RRuleScheduleBuilder cb = RRuleScheduleBuilder.rruleSchedule(rruleExpr);

        if (timeZoneId != null) {
            cb.inTimeZone(TimeZone.getTimeZone(timeZoneId));
        }

        return new TriggerPropertyBundle(cb, null, null);
    }

    /**
     * Returns {@code false} so that RRULE trigger properties are always loaded
     * via {@link #loadExtendedTriggerProperties(Connection, TriggerKey)} rather
     * than from an inlined bulk-select result set. Unlike the cron table, the
     * {@code RRULE_TRIGGERS} table is not part of {@code SELECT_BULK_TRIGGERS_BASE}
     * (adding it would collide on the shared {@code TIME_ZONE_ID} column), so the
     * dedicated per-trigger select is used, exactly as for the non-inlined
     * delegates.
     */
    public boolean hasInlinedResultSetProperties() {
        return false;
    }

    public int updateExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state,
            JobDetail jobDetail) throws SQLException, IOException {

        RRuleTrigger rruleTrigger = (RRuleTrigger) trigger;

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(Util.rtp(UPDATE_RRULE_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, rruleTrigger.getRRuleExpression());
            ps.setString(2, rruleTrigger.getTimeZone().getID());
            ps.setString(3, trigger.getKey().getName());
            ps.setString(4, trigger.getKey().getGroup());

            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }

}
