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
package org.quartz.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.quartz.utils.DirtyFlagMap;

import junit.framework.TestCase;

/**
 * Unit test for DirtyFlagMap.  These tests focus on making
 * sure the isDirty flag is set correctly.
 */
public class DirtyFlagMapTest extends TestCase {

    public void testClear() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        assertFalse(dirtyFlagMap.isDirty());
        
        dirtyFlagMap.clear();
        assertFalse(dirtyFlagMap.isDirty());
        dirtyFlagMap.put("X", "Y");
        dirtyFlagMap.clearDirtyFlag();
        dirtyFlagMap.clear();
        assertTrue(dirtyFlagMap.isDirty());
    }
    
    public void testPut() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        dirtyFlagMap.put("a", "Y");
        assertTrue(dirtyFlagMap.isDirty());
    }
    
    public void testRemove() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        dirtyFlagMap.put("a", "Y");
        dirtyFlagMap.clearDirtyFlag();
        
        dirtyFlagMap.remove("b");
        assertFalse(dirtyFlagMap.isDirty());

        dirtyFlagMap.remove("a");
        assertTrue(dirtyFlagMap.isDirty());
    }
    
    public void testEntrySetRemove() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        Set<Map.Entry<String, String>> entrySet = dirtyFlagMap.entrySet();
        dirtyFlagMap.remove("a");
        assertFalse(dirtyFlagMap.isDirty());
        dirtyFlagMap.put("a", "Y");
        dirtyFlagMap.clearDirtyFlag();
        entrySet.remove("b");
        assertFalse(dirtyFlagMap.isDirty());
        entrySet.remove(entrySet.iterator().next());
        assertTrue(dirtyFlagMap.isDirty());
    }

    public void testEntrySetRetainAll() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        Set<Map.Entry<String, String>> entrySet = dirtyFlagMap.entrySet();
        entrySet.retainAll(Collections.EMPTY_LIST);
        assertFalse(dirtyFlagMap.isDirty());
        dirtyFlagMap.put("a", "Y");
        dirtyFlagMap.clearDirtyFlag();
        entrySet.retainAll(Collections.singletonList(entrySet.iterator().next()));
        assertFalse(dirtyFlagMap.isDirty());
        entrySet.retainAll(Collections.EMPTY_LIST);
        assertTrue(dirtyFlagMap.isDirty());
    }
    
    public void testEntrySetRemoveAll() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        Set<Map.Entry<String, String>> entrySet = dirtyFlagMap.entrySet();
        entrySet.removeAll(Collections.EMPTY_LIST);
        assertFalse(dirtyFlagMap.isDirty());
        dirtyFlagMap.put("a", "Y");
        dirtyFlagMap.clearDirtyFlag();
        entrySet.removeAll(Collections.EMPTY_LIST);
        assertFalse(dirtyFlagMap.isDirty());
        entrySet.removeAll(Collections.singletonList(entrySet.iterator().next()));
        assertTrue(dirtyFlagMap.isDirty());
    }
    
    public void testEntrySetClear() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        Set<Map.Entry<String, String>> entrySet = dirtyFlagMap.entrySet();
        entrySet.clear();
        assertFalse(dirtyFlagMap.isDirty());
        dirtyFlagMap.put("a", "Y");
        dirtyFlagMap.clearDirtyFlag();
        entrySet.clear();
        assertTrue(dirtyFlagMap.isDirty());
    }        

    @SuppressWarnings("unchecked")
    public void testEntrySetIterator() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        Set<Map.Entry<String, String>> entrySet = dirtyFlagMap.entrySet();
        dirtyFlagMap.put("a", "A");
        dirtyFlagMap.put("b", "B");
        dirtyFlagMap.put("c", "C");
        dirtyFlagMap.clearDirtyFlag();
        Iterator<?> entrySetIter = entrySet.iterator();
        Map.Entry<?, ?> entryToBeRemoved = (Map.Entry<?, ?>)entrySetIter.next();
        String removedKey = (String)entryToBeRemoved.getKey();
        entrySetIter.remove();
        assertEquals(2, dirtyFlagMap.size());
        assertTrue(dirtyFlagMap.isDirty());
        assertFalse(dirtyFlagMap.containsKey(removedKey));
        dirtyFlagMap.clearDirtyFlag();
        Map.Entry<?, String> entry = (Map.Entry<?, String>)entrySetIter.next();
        entry.setValue("BB");
        assertTrue(dirtyFlagMap.isDirty());
        assertTrue(dirtyFlagMap.containsValue("BB"));
    }

    @SuppressWarnings("unchecked")
    public void testEntrySetToArray() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        Set<Map.Entry<String, String>> entrySet = dirtyFlagMap.entrySet();
        dirtyFlagMap.put("a", "A");
        dirtyFlagMap.put("b", "B");
        dirtyFlagMap.put("c", "C");
        dirtyFlagMap.clearDirtyFlag();
        Object[] array = entrySet.toArray();
        assertEquals(3, array.length);
        Map.Entry<?, String> entry = (Map.Entry<?, String>)array[0];
        entry.setValue("BB");
        assertTrue(dirtyFlagMap.isDirty());
        assertTrue(dirtyFlagMap.containsValue("BB"));
    }

    @SuppressWarnings("unchecked")
    public void testEntrySetToArrayWithArg() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        Set<Map.Entry<String, String>> entrySet = dirtyFlagMap.entrySet();
        dirtyFlagMap.put("a", "A");
        dirtyFlagMap.put("b", "B");
        dirtyFlagMap.put("c", "C");
        dirtyFlagMap.clearDirtyFlag();
        Object[] array = entrySet.toArray(new Map.Entry[] {});
        assertEquals(3, array.length);
        Map.Entry<?, String> entry = (Map.Entry<?, String>)array[0];
        entry.setValue("BB");
        assertTrue(dirtyFlagMap.isDirty());
        assertTrue(dirtyFlagMap.containsValue("BB"));
    }
    
    public void testKeySetClear() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        Set<?> keySet = dirtyFlagMap.keySet();
        keySet.clear();
        assertFalse(dirtyFlagMap.isDirty());
        dirtyFlagMap.put("a", "Y");
        dirtyFlagMap.clearDirtyFlag();
        keySet.clear();
        assertTrue(dirtyFlagMap.isDirty());
        assertEquals(0, dirtyFlagMap.size());
    }    
        
    public void testValuesClear() {
        DirtyFlagMap<String, String> dirtyFlagMap = new DirtyFlagMap<String, String>();
        Collection<?> values = dirtyFlagMap.values();
        values.clear();
        assertFalse(dirtyFlagMap.isDirty());
        dirtyFlagMap.put("a", "Y");
        dirtyFlagMap.clearDirtyFlag();
        values.clear();
        assertTrue(dirtyFlagMap.isDirty());
        assertEquals(0, dirtyFlagMap.size());
    }    
}
