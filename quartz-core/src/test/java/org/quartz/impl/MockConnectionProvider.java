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
package org.quartz.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.quartz.utils.ConnectionProvider;

/**
 * Mock implementation of a ConnectionProvider
 * that keeps track of the order of it methods calls
 * 
 * @author adahanne
 */
public class MockConnectionProvider implements ConnectionProvider {

	private String customProperty; 
	public static List<String> methodsCalled = new ArrayList<String>();
	
	public Connection getConnection() throws SQLException {
		methodsCalled.add("getConnection");
		throw new MockSQLException("getConnection correctly called on MockConnectionProvider");
	}

	public void shutdown() throws SQLException {
	}

	public void initialize() throws SQLException {
		methodsCalled.add("initialize");

	}

	public void setCustomProperty(String customProperty) {
		methodsCalled.add("setCustomProperty("+customProperty+")");
	}
	
}

class MockSQLException extends SQLException{
	public MockSQLException(String string) {
		super(string);
	}
	
}
