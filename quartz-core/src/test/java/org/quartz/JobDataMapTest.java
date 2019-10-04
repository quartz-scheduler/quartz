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
 */
package org.quartz;


/**
 * Unit test for JobDataMap serialization backwards compatibility.
 */
public class JobDataMapTest extends SerializationTestSupport {
    private static final String[] VERSIONS = new String[] {"1.4.5", "1.5.1", "2.1"};
    
    /**
     * Get the object to serialize when generating serialized file for future
     * tests, and against which to validate deserialized object.
     */
    @Override
    protected Object getTargetObject() {
        JobDataMap m = new JobDataMap();
        m.put("key", Integer.valueOf(5));
        return m;
    }
    
    /**
     * Get the Quartz versions for which we should verify
     * serialization backwards compatibility.
     */
    @Override
    protected String[] getVersions() {
        return VERSIONS;
    }
    
    /**
     * Verify that the target object and the object we just deserialized 
     * match.
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void verifyMatch(Object target, Object deserialized) {
        JobDataMap targetMap = (JobDataMap)target;
        JobDataMap deserializedMap = (JobDataMap)deserialized;
        
        assertNotNull(deserializedMap);
        assertEquals(targetMap.getWrappedMap(), deserializedMap.getWrappedMap());
        assertEquals(targetMap.getAllowsTransientData(), deserializedMap.getAllowsTransientData());
        assertEquals(targetMap.isDirty(), deserializedMap.isDirty());
    }
    
    public static void main(String[] args) throws Exception {
		new JobDataMapTest().writeJobDataFile("2.1");
	}
}
