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

import java.sql.SQLException;
import java.util.HashMap;

import org.quartz.AbstractJobStoreTest;
import org.quartz.spi.JobStore;

public class JdbcJobStoreTest extends AbstractJobStoreTest {

	private HashMap<String, JobStoreSupport> stores = new HashMap<String, JobStoreSupport>();
	
    public void testNothing() {
        // nothing
    }

    @Override
    protected JobStore createJobStore(String name) {
        try {
            JdbcQuartzTestUtilities.createDatabase(name);
            JobStoreTX jdbcJobStore = new JobStoreTX();
            jdbcJobStore.setDataSource(name);
            jdbcJobStore.setTablePrefix("QRTZ_");
            jdbcJobStore.setInstanceId("SINGLE_NODE_TEST");
            jdbcJobStore.setInstanceName(name);
            jdbcJobStore.setUseDBLocks(true);

            stores.put(name, jdbcJobStore);
            
            return jdbcJobStore;
        } catch (SQLException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected void destroyJobStore(String name) {
        try {
        	JobStoreSupport jdbcJobStore = stores.remove(name);
        	jdbcJobStore.shutdown();
        	
            JdbcQuartzTestUtilities.destroyDatabase(name);
        } catch (SQLException e) {
            throw new AssertionError(e);
        }
    }
}
