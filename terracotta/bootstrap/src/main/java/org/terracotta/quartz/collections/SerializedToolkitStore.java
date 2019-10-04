/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved. Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.quartz.collections;

import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SerializedToolkitStore<K, V extends Serializable> implements ToolkitStore<K, V> {
  private final ToolkitStore<String, V> toolkitStore;

  public SerializedToolkitStore(ToolkitStore toolkitMap) {
    this.toolkitStore = toolkitMap;
  }

  @Override
  public int size() {
    return this.toolkitStore.size();
  }

  @Override
  public boolean isEmpty() {
    return this.toolkitStore.isEmpty();
  }

  private static String serializeToString(Object key) {
    try {
      return SerializationHelper.serializeToString(key);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Object deserializeFromString(String key) {
    try {
      return SerializationHelper.deserializeFromString(key);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean containsKey(Object key) {
    return this.toolkitStore.containsKey(serializeToString(key));
  }

  @Override
  public V get(Object key) {
    return this.toolkitStore.get(serializeToString(key));
  }

  @Override
  public V put(K key, V value) {
    return this.toolkitStore.put(serializeToString(key), value);
  }

  @Override
  public V remove(Object key) {
    return this.toolkitStore.remove(serializeToString(key));
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    Map<String, V> tempMap = new HashMap<String, V>();
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      tempMap.put(serializeToString(entry.getKey()), entry.getValue());
    }

    toolkitStore.putAll(tempMap);
  }

  @Override
  public void clear() {
    toolkitStore.clear();
  }

  @Override
  public Set<K> keySet() {
    return new ToolkitKeySet(toolkitStore.keySet());
  }

  @Override
  public boolean isDestroyed() {
    return toolkitStore.isDestroyed();
  }

  @Override
  public void destroy() {
    toolkitStore.destroy();
  }

  @Override
  public String getName() {
    return toolkitStore.getName();
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    return toolkitStore.createLockForKey(serializeToString(key));
  }

  @Override
  public void removeNoReturn(Object key) {
    toolkitStore.removeNoReturn(serializeToString(key));
  }

  @Override
  public void putNoReturn(K key, V value) {
    toolkitStore.putNoReturn(serializeToString(key), value);
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    HashSet<String> tempSet = new HashSet<String>();
    for (K key : keys) {
      tempSet.add(serializeToString(key));
    }

    Map<String, V> m = toolkitStore.getAll(tempSet);
    Map<K, V> tempMap = m.isEmpty() ? Collections.EMPTY_MAP : new HashMap<K, V>();

    for (Entry<String, V> entry : m.entrySet()) {
      tempMap.put((K) deserializeFromString(entry.getKey()), entry.getValue());
    }

    return tempMap;
  }

  @Override
  public Configuration getConfiguration() {
    return toolkitStore.getConfiguration();
  }

  @Override
  public void setConfigField(String name, Serializable value) {
    toolkitStore.setConfigField(name, value);
  }

  @Override
  public boolean containsValue(Object value) {
    return toolkitStore.containsValue(value);
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return toolkitStore.putIfAbsent(serializeToString(key), value);
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    return new ToolkitEntrySet(this.toolkitStore.entrySet());
  }

  @Override
  public Collection<V> values() {
    return this.toolkitStore.values();
  }

  @Override
  public boolean remove(Object key, Object value) {
    return this.toolkitStore.remove(serializeToString(key), value);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return this.toolkitStore.replace(serializeToString(key), oldValue, newValue);
  }

  @Override
  public V replace(K key, V value) {
    return this.toolkitStore.replace(serializeToString(key), value);
  }

  @Override
  public boolean isBulkLoadEnabled() {
    return this.toolkitStore.isBulkLoadEnabled();
  }

  @Override
  public boolean isNodeBulkLoadEnabled() {
    return this.toolkitStore.isNodeBulkLoadEnabled();
  }

  @Override
  public void setNodeBulkLoadEnabled(boolean enabledBulkLoad) {
    this.toolkitStore.setNodeBulkLoadEnabled(enabledBulkLoad);
  }

  @Override
  public void waitUntilBulkLoadComplete() throws InterruptedException {
    this.toolkitStore.waitUntilBulkLoadComplete();
  }

  private static class ToolkitEntrySet<K, V> implements Set<java.util.Map.Entry<K, V>> {
    private final Set<java.util.Map.Entry<String, V>> set;

    public ToolkitEntrySet(Set<java.util.Map.Entry<String, V>> set) {
      this.set = set;
    }

    @Override
    public int size() {
      return set.size();
    }

    @Override
    public boolean isEmpty() {
      return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      if (!(o instanceof Map.Entry)) { return false; }

      Map.Entry<K, V> entry = (java.util.Map.Entry<K, V>) o;
      ToolkitMapEntry<String, V> toolkitEntry = null;
      toolkitEntry = new ToolkitMapEntry<String, V>(serializeToString(entry.getKey()), entry.getValue());
      return this.set.contains(toolkitEntry);
    }

    @Override
    public Iterator<java.util.Map.Entry<K, V>> iterator() {
      return new ToolkitEntryIterator<K, V>(set.iterator());
    }

    @Override
    public Object[] toArray() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(java.util.Map.Entry<K, V> e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends java.util.Map.Entry<K, V>> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }

  private static class ToolkitEntryIterator<K, V> implements Iterator<Map.Entry<K, V>> {

    private final Iterator<Map.Entry<String, V>> iter;

    public ToolkitEntryIterator(Iterator<Map.Entry<String, V>> iter) {
      this.iter = iter;
    }

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public java.util.Map.Entry<K, V> next() {
      Map.Entry<String, V> entry = iter.next();
      if (entry == null) { return null; }
      return new ToolkitMapEntry(deserializeFromString(entry.getKey()), entry.getValue());
    }

    @Override
    public void remove() {
      iter.remove();
    }

  }

  private static class ToolkitMapEntry<K, V> implements Map.Entry<K, V> {
    private final K k;
    private final V v;

    public ToolkitMapEntry(K k, V v) {
      this.k = k;
      this.v = v;
    }

    @Override
    public K getKey() {
      return k;
    }

    @Override
    public V getValue() {
      return v;
    }

    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException();
    }

  }

  private static class ToolkitKeySet<K> implements Set<K> {

    private final Set<String> set;

    public ToolkitKeySet(Set<String> set) {
      this.set = set;
    }

    @Override
    public int size() {
      return set.size();
    }

    @Override
    public boolean isEmpty() {
      return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return set.contains(serializeToString(o));
    }

    @Override
    public Iterator<K> iterator() {
      return new ToolkitKeyIterator<K>(set.iterator());
    }

    @Override
    public Object[] toArray() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(K e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends K> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }

  private static class ToolkitKeyIterator<K> implements Iterator<K> {

    private final Iterator<String> iter;

    public ToolkitKeyIterator(Iterator<String> iter) {
      this.iter = iter;
    }

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public K next() {
      String k = iter.next();
      if (k == null) { return null; }
      return (K) deserializeFromString(k);
    }

    @Override
    public void remove() {
      iter.remove();
    }

  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor attrExtractor) {
     this.toolkitStore.setAttributeExtractor(attrExtractor);
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    throw new UnsupportedOperationException();
  }

}
