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
package org.quartz.impl.matchers;

import org.quartz.JobKey;
import org.quartz.TriggerKey;

import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.impl.matchers.GroupMatcher.anyJobGroup;
import static org.quartz.impl.matchers.GroupMatcher.anyTriggerGroup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for CronScheduleBuilder.
 * 
 * @author jhouse
 *
 */
class GroupMatcherTest {
	
    @Test
	void testAnyGroupMatchers() {

        TriggerKey tKey = triggerKey("booboo", "baz");
        JobKey jKey = jobKey("frumpwomp", "bazoo");

        GroupMatcher tgm = anyTriggerGroup();
        GroupMatcher jgm = anyJobGroup();

        Assertions.assertTrue(tgm.isMatch(tKey), "Expected match on trigger group");
        Assertions.assertTrue(jgm.isMatch(jKey), "Expected match on job group");

	}

}
