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

package org.quartz.utils.counter.sampled;

import org.quartz.utils.counter.Counter;

/**
 * Interface of a sampled counter -- a counter that keeps sampled values
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 * 
 */
public interface SampledCounter extends Counter {
    /**
     * Shutdown this counter
     */
    void shutdown();

    /**
     * Returns the most recent sampled value
     * 
     * @return Value of the most recent sampled value
     */
    TimeStampedCounterValue getMostRecentSample();

    /**
     * Returns all samples in history
     * 
     * @return An array containing the TimeStampedCounterValue's
     */
    TimeStampedCounterValue[] getAllSampleValues();

    /**
     * Returns the current value of the counter and resets it to 0
     * 
     * @return current value of the counter
     */
    long getAndReset();

}
