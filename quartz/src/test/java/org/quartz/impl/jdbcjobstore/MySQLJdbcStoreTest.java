package org.quartz.impl.jdbcjobstore;

public class MySQLJdbcStoreTest extends JdbcJobStoreTest {
    protected JdbcQuartzTestUtilities.DatabaseType getDatabaseType() {
        return JdbcQuartzTestUtilities.DatabaseType.MYSQL;
    }
}
