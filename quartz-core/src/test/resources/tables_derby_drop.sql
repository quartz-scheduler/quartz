-- 
-- Apache Derby scripts by Steve Stewart, updated by Ronald Pomeroy
-- Based on Srinivas Venkatarangaiah's file for Cloudscape
-- 
-- Known to work with Apache Derby 10.0.2.1, or 10.6.2.1
--
-- Updated by Zemian Deng <saltnlight5@gmail.com> on 08/21/2011
--   * Fixed nullable fields on qrtz_simprop_triggers table. 
--   * Added Derby QuickStart comments and drop tables statements.
--
-- DerbyDB + Quartz Quick Guide:
-- * Derby comes with Oracle JDK! For Java6, it default install into C:/Program Files/Sun/JavaDB on Windows.
-- 1. Create a derby.properties file under JavaDB directory, and have the following:
--    derby.connection.requireAuthentication = true
--    derby.authentication.provider = BUILTIN
--    derby.user.quartz2=quartz2123
-- 2. Start the DB server by running bin/startNetworkServer script.
-- 3. On a new terminal, run bin/ij tool to bring up an SQL prompt, then run:
--    connect 'jdbc:derby://localhost:1527/quartz2;user=quartz2;password=quartz2123;create=true';
--    run 'quartz/docs/dbTables/tables_derby.sql';
-- Now in quartz.properties, you may use these properties:
--    org.quartz.dataSource.quartzDataSource.driver = org.apache.derby.jdbc.ClientDriver
--    org.quartz.dataSource.quartzDataSource.URL = jdbc:derby://localhost:1527/quartz2
--    org.quartz.dataSource.quartzDataSource.user = quartz2
--    org.quartz.dataSource.quartzDataSource.password = quartz2123
--

-- Auto drop and reset tables 
-- Derby doesn't support if exists condition on table drop, so user must manually do this step if needed to.
drop table qrtz_fired_triggers;
drop table qrtz_paused_trigger_grps;
drop table qrtz_scheduler_state;
drop table qrtz_locks;
drop table qrtz_simple_triggers;
drop table qrtz_simprop_triggers;
drop table qrtz_cron_triggers;
drop table qrtz_blob_triggers;
drop table qrtz_triggers;
drop table qrtz_job_details;
drop table qrtz_calendars;

