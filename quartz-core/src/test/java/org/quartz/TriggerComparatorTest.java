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

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.DateBuilder.IntervalUnit.MINUTE;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.quartz.spi.OperableTrigger;

public class TriggerComparatorTest extends TestCase {

    public void testTriggerSort() {
        
        // build trigger in expected sort order
        Trigger t1 = newTrigger().withIdentity("a").build();
        Trigger t2 = newTrigger().withIdentity("b").build();
        Trigger t3 = newTrigger().withIdentity("c").build();
        Trigger t4 = newTrigger().withIdentity("a", "a").build();
        Trigger t5 = newTrigger().withIdentity("a", "b").build();
        Trigger t6 = newTrigger().withIdentity("a", "c").build();

        List<Trigger> ts = new LinkedList<Trigger>();
        // add triggers to list in somewhat randomized order
        ts.add(t5);
        ts.add(t6);
        ts.add(t4);
        ts.add(t3);
        ts.add(t1);
        ts.add(t2);

        // sort the list
        Collections.sort(ts);
        
        // check the order of the list
        assertEquals(t1, ts.get(0));
        assertEquals(t2, ts.get(1));
        assertEquals(t3, ts.get(2));
        assertEquals(t4, ts.get(3));
        assertEquals(t5, ts.get(4));
        assertEquals(t6, ts.get(5));
    }
    
    public void testTriggerTimeSort() {
        
        
        // build trigger in expected sort order
        Trigger t1 = newTrigger().withIdentity("a").startAt(futureDate(1, MINUTE)).build();
        ((OperableTrigger)t1).computeFirstFireTime(null);
        Trigger t2 = newTrigger().withIdentity("b").startAt(futureDate(2, MINUTE)).build();
        ((OperableTrigger)t2).computeFirstFireTime(null);
        Trigger t3 = newTrigger().withIdentity("c").startAt(futureDate(3, MINUTE)).build();
        ((OperableTrigger)t3).computeFirstFireTime(null);
        Trigger t4 = newTrigger().withIdentity("d").startAt(futureDate(5, MINUTE)).withPriority(7).build();
        ((OperableTrigger)t4).computeFirstFireTime(null);
        Trigger t5 = newTrigger().withIdentity("e").startAt(futureDate(5, MINUTE)).build();
        ((OperableTrigger)t5).computeFirstFireTime(null);
        Trigger t6 = newTrigger().withIdentity("g").startAt(futureDate(5, MINUTE)).build();
        ((OperableTrigger)t6).computeFirstFireTime(null);
        Trigger t7 = newTrigger().withIdentity("h").startAt(futureDate(5, MINUTE)).withPriority(2).build();
        ((OperableTrigger)t7).computeFirstFireTime(null);
        Trigger t8 = newTrigger().withIdentity("i").startAt(futureDate(6, MINUTE)).build();
        ((OperableTrigger)t8).computeFirstFireTime(null);
        Trigger t9 = newTrigger().withIdentity("j").startAt(futureDate(7, MINUTE)).build();
        ((OperableTrigger)t9).computeFirstFireTime(null);

        List<Trigger> ts = new LinkedList<Trigger>();
        // add triggers to list in somewhat randomized order
        ts.add(t5);
        ts.add(t9);
        ts.add(t6);
        ts.add(t8);
        ts.add(t4);
        ts.add(t3);
        ts.add(t1);
        ts.add(t7);
        ts.add(t2);
        
        // sort the list
        Collections.sort(ts);
        
        // check the order of the list
        assertEquals(t1, ts.get(0));
        assertEquals(t2, ts.get(1));
        assertEquals(t3, ts.get(2));
        assertEquals(t4, ts.get(3));
        assertEquals(t5, ts.get(4));
        assertEquals(t6, ts.get(5));
        assertEquals(t7, ts.get(6));
        assertEquals(t8, ts.get(7));
        assertEquals(t9, ts.get(8));
    }

}

