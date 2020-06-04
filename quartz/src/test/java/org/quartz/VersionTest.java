/* 
 * Copyright 2007-2009 Terracotta, Inc. 
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

import java.util.regex.Pattern;
import junit.framework.TestCase;

import org.quartz.core.QuartzScheduler;

public class VersionTest extends TestCase {
    private static final String SNAPSHOT_SUFFIX = "-atlassian-1-SNAPSHOT";

    public void testVersionParsing() {
        assertNonNegativeInteger(QuartzScheduler.getVersionMajor());
        assertNonNegativeInteger(QuartzScheduler.getVersionMinor());

        String iter = QuartzScheduler.getVersionIteration();
        assertNotNull(iter);
        if (iter.endsWith(SNAPSHOT_SUFFIX)) {
            assertNonNegativeInteger(iter.substring(0, iter.length() - SNAPSHOT_SUFFIX.length()));
        }
        else {
            assertNonNegativeInteger(iter);
        }
    }

    private void assertNonNegativeInteger(String s) {
        assertNotNull(s);
        boolean parsed = false;
        int intVal = -1;
        try {
            intVal = Integer.parseInt(s);
            parsed = true;
        } catch (NumberFormatException e) {}

        assertTrue(parsed);
        assertTrue(intVal >= 0);
    }
}

