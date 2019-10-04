/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.quartz.JobKey;
import org.quartz.spi.ClassLoadHelper;
import org.slf4j.Logger;

/**
 * Quartz JDBC delegate for DB2 v6 databases. <code>select count(name)</code>
 * had to be replaced with <code>select count(*)</code>.
 * 
 * @author Martin Renner
 * @author James House
 */
public class DB2v6Delegate extends StdJDBCDelegate {
    @SuppressWarnings("hiding")
    public static final String SELECT_NUM_JOBS = "SELECT COUNT(*) FROM "
            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS
            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    @SuppressWarnings("hiding")
    public static final String SELECT_NUM_TRIGGERS_FOR_JOB = "SELECT COUNT(*) FROM "
            + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS
            + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " 
            + COL_JOB_NAME
            + " = ? AND " + COL_JOB_GROUP + " = ?";

    @SuppressWarnings("hiding")
    public static final String SELECT_NUM_TRIGGERS = "SELECT COUNT(*) FROM "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS
            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    @SuppressWarnings("hiding")
    public static final String SELECT_NUM_CALENDARS = "SELECT COUNT(*) FROM "
            + TABLE_PREFIX_SUBST + TABLE_CALENDARS
            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    @Override
    public int selectNumJobs(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            int count = 0;
            ps = conn.prepareStatement(rtp(SELECT_NUM_JOBS));
            rs = ps.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }

            return count;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    @Override           
    public int selectNumTriggersForJob(Connection conn, JobKey jobKey) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_NUM_TRIGGERS_FOR_JOB));
            ps.setString(1, jobKey.getName());
            ps.setString(2, jobKey.getGroup());
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    @Override
    public int selectNumTriggers(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            int count = 0;
            ps = conn.prepareStatement(rtp(SELECT_NUM_TRIGGERS));
            rs = ps.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }

            return count;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    @Override           
    public int selectNumCalendars(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            int count = 0;
            ps = conn.prepareStatement(rtp(SELECT_NUM_CALENDARS));
            rs = ps.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }

            return count;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }
}

// EOF
