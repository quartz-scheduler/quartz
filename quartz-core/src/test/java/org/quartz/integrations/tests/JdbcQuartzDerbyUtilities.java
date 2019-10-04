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
package org.quartz.integrations.tests;



import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.quartz.utils.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JdbcQuartzDerbyUtilities {

    private static final Logger LOG = LoggerFactory
            .getLogger(JdbcQuartzDerbyUtilities.class);

    private static final String DATABASE_DRIVER_CLASS = "org.apache.derby.jdbc.ClientDriver";
    public static final String DATABASE_PORT = System.getProperty("test.databasePort", "1527");;
    public static final String DATABASE_CONNECTION_PREFIX;

    private static final List<String> DATABASE_SETUP_STATEMENTS;
    private static final List<String> DATABASE_TEARDOWN_STATEMENTS;
    private static final String DERBY_DIRECTORY;

    private final static Properties PROPS = new Properties();

    static {

        String derbyDirectory;
        if (System.getProperty("buildDirectory") != null) {
            // running the tests from maven, the db will be stored in target/
            derbyDirectory = System.getProperty("buildDirectory")+"/quartzTestDb";
            LOG.info("running the tests with maven, the db will be stored in "+derbyDirectory);
        } else {
            derbyDirectory = System.getProperty("java.io.tmpdir") + "quartzTestDb";
            LOG.info("not using maven, the db will be stored in "+derbyDirectory);
        }
        DERBY_DIRECTORY = derbyDirectory;

        DATABASE_CONNECTION_PREFIX = "jdbc:derby://localhost:" + DATABASE_PORT + "/"
                + DERBY_DIRECTORY + ";create=true";

    	PROPS.setProperty("user","quartz");
    	PROPS.setProperty("password","quartz");
    	
    	
        try {
            Class.forName(DATABASE_DRIVER_CLASS).newInstance();
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        } catch (InstantiationException e) {
            throw new AssertionError(e);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }

        List<String> setup = new ArrayList<String>();
        String setupScript;
        try {
            InputStream setupStream = DerbyConnectionProvider.class
                    .getClassLoader().getResourceAsStream("org/quartz/impl/jdbcjobstore/tables_derby.sql");
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(setupStream, "US-ASCII"));
                StringBuilder sb = new StringBuilder();
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    } else if (!line.startsWith("--")) {
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
                setup.add(command);
            }
        }
        DATABASE_SETUP_STATEMENTS = setup;
        
        
        List<String> tearDown = new ArrayList<String>();
        String tearDownScript;
        try {
            InputStream tearDownStream = DerbyConnectionProvider.class
                    .getClassLoader().getResourceAsStream("tables_derby_drop.sql");
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(tearDownStream, "US-ASCII"));
                StringBuilder sb = new StringBuilder();
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    } else if (!line.startsWith("--")) {
                        sb.append(line).append("\n");
                    }
                }
                tearDownScript = sb.toString();
            } finally {
                tearDownStream.close();
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        for (String command : tearDownScript.split(";")) {
            if (!command.matches("\\s*")) {
            	tearDown.add(command);
            }
        }
        DATABASE_TEARDOWN_STATEMENTS = tearDown;
        
        
    }

    public static void createDatabase() throws SQLException {

        File derbyDirectory = new File(DERBY_DIRECTORY);
        delete(derbyDirectory);

    	Connection conn = DriverManager.getConnection(DATABASE_CONNECTION_PREFIX ,PROPS);
        try {
            Statement statement = conn.createStatement();
            for (String command : DATABASE_SETUP_STATEMENTS) {
                statement.addBatch(command);
            }
            statement.executeBatch();
        }
        finally {
            conn.close();
        }
    }

    
	public static int triggersInAcquiredState() throws SQLException {
		int triggersInAcquiredState = 0;
		Connection conn = DriverManager.getConnection(DATABASE_CONNECTION_PREFIX, PROPS);
		try {
			Statement statement = conn.createStatement();
			ResultSet result = statement.executeQuery("SELECT count( * ) FROM QRTZ_TRIGGERS WHERE TRIGGER_STATE = 'ACQUIRED' ");
			while (result.next()) { 
				triggersInAcquiredState = result.getInt(1);
			}
		} finally {
			conn.close();
		}
		return triggersInAcquiredState;
	}
    
	
	public static BigDecimal timesTriggered(String triggerName,String triggerGroup) throws SQLException {
		BigDecimal timesTriggered = BigDecimal.ZERO;
		Connection conn = DriverManager.getConnection(DATABASE_CONNECTION_PREFIX, PROPS);
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT TIMES_TRIGGERED FROM QRTZ_SIMPLE_TRIGGERS WHERE TRIGGER_NAME = ? AND TRIGGER_GROUP = ? ");
			ps.setString(1, triggerName);
			ps.setString(2, triggerGroup);
			ResultSet result = ps.executeQuery();
			result.next(); 
			timesTriggered = result.getBigDecimal(1);
		} finally {
			conn.close();
		}
		return timesTriggered;
	}
	
    public static void destroyDatabase() throws SQLException {
    	Connection conn = DriverManager.getConnection(DATABASE_CONNECTION_PREFIX ,PROPS);
        try {
            Statement statement = conn.createStatement();
            for (String command : DATABASE_TEARDOWN_STATEMENTS) {
                statement.addBatch(command);
            }
            statement.executeBatch();
        }
        finally {
            conn.close();
        }

        File derbyDirectory = new File(DERBY_DIRECTORY);
        delete(derbyDirectory);
    }

    static class DerbyConnectionProvider implements ConnectionProvider {



        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(DATABASE_CONNECTION_PREFIX , PROPS);
        }

        public void shutdown() throws SQLException {
            // nothing to do
        }

		@Override
		public void initialize() throws SQLException {
			// nothing to do
		}
    }

    private JdbcQuartzDerbyUtilities() {
        // not instantiable
    }

    static void delete(File f)  {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            LOG.debug("Failed to delete file: " + f +" certainly because it does not exist yet");
    }

}
