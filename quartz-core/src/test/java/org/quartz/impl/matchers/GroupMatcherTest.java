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

import junit.framework.Assert;
import junit.framework.TestCase;
import org.quartz.JobKey;
import org.quartz.TriggerKey;

import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.impl.matchers.GroupMatcher.anyJobGroup;
import static org.quartz.impl.matchers.GroupMatcher.anyTriggerGroup;

/**
 * Unit test for CronScheduleBuilder.
 * 
 * @author jhouse
 *
 */
public class GroupMatcherTest extends TestCase {
	
	public void testAnyGroupMatchers() {

        TriggerKey tKey = triggerKey("booboo", "baz");
        JobKey jKey = jobKey("frumpwomp", "bazoo");

        GroupMatcher tgm = anyTriggerGroup();
        GroupMatcher jgm = anyJobGroup();

        Assert.assertTrue("Expected match on trigger group", tgm.isMatch(tKey));
        Assert.assertTrue("Expected match on job group", jgm.isMatch(jKey));

	}

}
