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

import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class JdbcQuartzTestUtilities {

    public enum DatabaseType {
        DERBY("org/quartz/impl/jdbcjobstore/tables_derby.sql", StdJDBCDelegate.class.getName()),
        MSSQL("org/quartz/impl/jdbcjobstore/tables_sqlServer.sql", MSSQLDelegate.class.getName()),
        MARIADB("org/quartz/impl/jdbcjobstore/tables_mysql.sql", StdJDBCDelegate.class.getName());

        private final String scriptResource;
        private final String delegateClassName;

        DatabaseType(String scriptResource) {
            this(scriptResource, StdJDBCDelegate.class.getName());
        }

        DatabaseType(String scriptResource, String delegateClassName) {
            this.scriptResource = scriptResource;
            this.delegateClassName = delegateClassName;
        }

        public String getDelegateClassName() {
            return this.delegateClassName;
        }
    }

    private static final Map<DatabaseType, List<String>> DATABASE_SETUP_SCRIPTS = new EnumMap<>(DatabaseType.class);

    private static List<String> getDatabaseSetupScript(DatabaseType type) {
        return DATABASE_SETUP_SCRIPTS.computeIfAbsent(type, t -> {
            List<String> commandList = new ArrayList<>();
            String setupScript;
            try {
                InputStream setupStream = JdbcQuartzTestUtilities.class.getClassLoader()
                        .getResourceAsStream(t.scriptResource);
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(setupStream, "US-ASCII"));
                    StringBuilder sb = new StringBuilder();
                    while (true) {
                        String line = r.readLine();
                        if (line == null) {
                            break;
                        } else if (!line.startsWith("--") && !line.startsWith("#")) {
                            // update script for some database like sql server to be executable with jdbc
                            sb.append(line).append("\n");
                        }
                    }
                    setupScript = sb.toString();
                } finally {
                    setupStream.close();
                }
            } catch (IOException e) {
                throw new AssertionError(e);
            }

            for (String command : setupScript.split(";")) {
                if (!command.matches("\\s*")) {
                    commandList.add(command);
                }
            }
            return commandList;

        });
    }

    public static void createDatabase(String name, DatabaseType databaseType) throws SQLException {
        switch (databaseType) {
        case DERBY:
            DBConnectionManager.getInstance().addConnectionProvider(name, new DerbyEmbeddedConnectionProvider(name));
            break;
        case MYSQL:
        case MSSQL:
            DBConnectionManager.getInstance().addConnectionProvider(name,
                    new TestContainerEmbeddedConnectionProvider(databaseType, name));
            break;
        case MARIADB:
            DBConnectionManager.getInstance().addConnectionProvider(name,
                    new TestContainerEmbeddedConnectionProvider("jdbc:tc:mariadb:latest:///" + name));
            break;
        default:
            throw new AssertionError("Unsupported database type: " + databaseType);
        }
    }

    public static void destroyDatabase(String name, DatabaseType databaseType) throws SQLException {
        switch (databaseType) {
        case DERBY:
            try {
                DriverManager.getConnection("jdbc:derby:memory:" + name + ";drop=true").close();
            } catch (SQLException e) {
                if (!("Database 'memory:" + name + "' dropped.").equals(e.getMessage())) {
                    throw e;
                }
            }
            break;
        case MYSQL:
        case MSSQL:
            shutdownDatabase(name, databaseType);
            break;
        case MARIADB:
                shutdownDatabase(name, databaseType);
                break;
        default:
            throw new AssertionError("Unsupported database type: " + databaseType);
        }
    }

    public static void shutdownDatabase(String name, DatabaseType databaseType) throws SQLException {
        switch (databaseType) {
        case DERBY:
            try {
                DriverManager.getConnection("jdbc:derby:;shutdown=true").close();
            } catch (SQLException e) {
                if (!("Derby system shutdown.").equals(e.getMessage())) {
                    throw e;
                }
            }
            break;
        case MYSQL:
        case MSSQL:
            DBConnectionManager.getInstance().shutdown(name);
            break;
        case MARIADB:
                DBConnectionManager.getInstance().shutdown(name);
                break;
        default:
            throw new AssertionError("Unsupported database type: " + databaseType);
        }
    }

    static class DerbyEmbeddedConnectionProvider implements ConnectionProvider {
    
        private final String databaseName;

        DerbyEmbeddedConnectionProvider(String name) throws SQLException {
            try {
                Class.forName("org.apache.derby.jdbc.EmbeddedDriver").getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new AssertionError(e);
            }           
            this.databaseName = name;
            Connection conn = DriverManager.getConnection("jdbc:derby:memory:" + databaseName + ";create=true");
            try {
                Statement statement = conn.createStatement();
                for (String command : getDatabaseSetupScript(DatabaseType.DERBY)) {
                    statement.addBatch(command);
                }
                statement.executeBatch();
            } finally {
                conn.close();
            }
        }

        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection("jdbc:derby:memory:" + databaseName);
        }

        public void shutdown() throws SQLException {
            // nothing to do
        }

        public void initialize() throws SQLException {
            // nothing to do
        }
    }

    static class TestContainerEmbeddedConnectionProvider implements ConnectionProvider {

        private final String jdbcUrl;
        //we keep a connection open to keep the testcontainer container alive
        private final Connection conn;

        TestContainerEmbeddedConnectionProvider(DatabaseType databaseType, String name) throws SQLException {
            if (databaseType == DatabaseType.MYSQL) {
                this.jdbcUrl = "jdbc:tc:mysql:8:///" + name;
            } else if (databaseType == DatabaseType.MSSQL) {
                this.jdbcUrl = "jdbc:tc:sqlserver:latest:///" + name;
            } else {
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
            }
            this.conn = DriverManager.getConnection(this.jdbcUrl);

            Statement statement = conn.createStatement();
            if (databaseType == DatabaseType.MYSQL) {
                for (String command : getDatabaseSetupScript(DatabaseType.MYSQL)) {
                    statement.addBatch(command);
                }
            } else {
                for (String command : getDatabaseSetupScript(DatabaseType.MSSQL)) {
                    statement.addBatch(command.replace("GO", ";").replace("[enter_db_name_here]", "[master]"));
                }
            }
            statement.executeBatch();
        }

        TestContainerEmbeddedConnectionProvider(String jdbcUrl) throws SQLException {
            this.jdbcUrl = jdbcUrl;
            this.conn = DriverManager.getConnection(this.jdbcUrl);

            Statement statement = conn.createStatement();
            if(jdbcUrl.contains("sqlserver")) {
                for (String command : getDatabaseSetupScript(DatabaseType.MSSQL)) {
                    statement.addBatch(command.replace("GO", ";").replace("[enter_db_name_here]", "[master]"));
                }
            }
            else if(jdbcUrl.contains("mariadb")) {
                for (String command : getDatabaseSetupScript(DatabaseType.MARIADB)) {
                    statement.addBatch(command);
                }
            }
            statement.executeBatch();
        }

        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(this.jdbcUrl);
        }

        public void shutdown() throws SQLException {
            // last connection closed shutdown testcontainer container
            if (!conn.isClosed()) {
                conn.close();
            }
        }

        public void initialize() throws SQLException {
            // nothing to do
        }
    }

    private JdbcQuartzTestUtilities() {
        // not instantiable
    }
}
