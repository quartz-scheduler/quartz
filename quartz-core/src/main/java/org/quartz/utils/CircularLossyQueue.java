/**
 *  All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.quartz.utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of a CircularQueue data-structure.
 * When the number of items added exceeds the maximum capacity, items that were
 * added first are lost.
 * 
 * @param <T>
 *            Type of the item's to add in this queue
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class CircularLossyQueue<T> {
    private final AtomicReference<T>[] circularArray;
    private final int maxSize;

    private final AtomicLong currentIndex = new AtomicLong(-1);

    /**
     * Constructs the circular queue with the specified capacity
     * 
     * @param size
     */
    @SuppressWarnings("unchecked")
    public CircularLossyQueue(int size) {
        this.circularArray = new AtomicReference[size];
        for (int i = 0; i < size; i++) {
            this.circularArray[i] = new AtomicReference<T>();
        }
        this.maxSize = size;
    }

    /**
     * Adds a new item
     * 
     * @param newVal
     */
    public void push(T newVal) {
        int index = (int) (currentIndex.incrementAndGet() % maxSize);
        circularArray[index].set(newVal);
    }

    /**
     * Returns an array of the current elements in the queue. The order of
     * elements is in reverse order of the order items were added.
     * 
     * @param type
     * @return An array containing the current elements in the queue. The first
     *         element of the array is the tail of the queue and the last
     *         element is the head of the queue
     */
    public T[] toArray(T[] type) {
        System.getProperties();

        if (type.length > maxSize) {
            throw new IllegalArgumentException("Size of array passed in cannot be greater than " + maxSize);
        }

        int curIndex = getCurrentIndex();
        for (int k = 0; k < type.length; k++) {
            int index = getIndex(curIndex - k);
            type[k] = circularArray[index].get();
        }
        return type;
    }

    private int getIndex(int index) {
        return (index < 0 ? index + maxSize : index);
    }

    /**
     * Returns value at the tail of the queue
     * 
     * @return Value at the tail of the queue
     */
    public T peek() {
        if (depth() == 0) {
            return null;
        }
        return circularArray[getIndex(getCurrentIndex())].get();
    }

    /**
     * Returns true if the queue is empty, otherwise false
     * 
     * @return true if the queue is empty, false otherwise
     */
    public boolean isEmtpy() {
        return depth() == 0;
    }

    private int getCurrentIndex() {
        return (int) (currentIndex.get() % maxSize);
    }

    /**
     * Returns the number of items currently in the queue
     * 
     * @return the number of items in the queue
     */
    public int depth() {
        long currInd = currentIndex.get() + 1;
        return currInd >= maxSize ? maxSize : (int) currInd;
    }
}
