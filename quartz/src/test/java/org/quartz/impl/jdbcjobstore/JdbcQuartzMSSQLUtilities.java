/* 
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024 
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.testcontainers.containers.MSSQLServerContainer;

/**
 * A utility class to create a database for Quartz MSSQL test.
 *
 * @author Arnaud Mergey
 */

public final class JdbcQuartzMSSQLUtilities {
	private static final List<String> DATABASE_SETUP_STATEMENTS;
	static {
		List<String> setup = new ArrayList<String>();
		String setupScript;
		try {
			InputStream setupStream = MSSQLDelegate.class.getClassLoader()
					.getResourceAsStream("org/quartz/impl/jdbcjobstore/tables_sqlServer.sql");
			try {
				BufferedReader r = new BufferedReader(new InputStreamReader(setupStream, "US-ASCII"));
				StringBuilder sb = new StringBuilder();
				while (true) {
					String line = r.readLine();
					if (line == null) {
						break;
					} else if (!line.startsWith("--")) {
						sb.append(line.replace("GO", ";").replace("[enter_db_name_here]", "[master]")).append("\n");
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
	}

	public static void createDatabase(MSSQLServerContainer<?> container) throws SQLException {
		Connection conn = container.createConnection("");
		try {
			Statement statement = conn.createStatement();
			for (String command : DATABASE_SETUP_STATEMENTS) {
				statement.addBatch(command);
			}
			statement.executeBatch();
		} finally {
			conn.close();
		}
	}
}