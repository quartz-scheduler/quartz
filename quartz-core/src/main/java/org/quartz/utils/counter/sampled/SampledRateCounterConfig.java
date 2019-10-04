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
 * An implementation of {@link SampledCounterConfig}
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 * 
 */
public class SampledRateCounterConfig extends SampledCounterConfig {

    private final long initialNumeratorValue;
    private final long initialDenominatorValue;

    /**
     * Constructor accepting the interval time in seconds, history-size and
     * whether counters should reset on each sample or not.
     * Initial values of both numerator and denominator are zeroes
     * 
     * @param intervalSecs
     * @param historySize
     * @param isResetOnSample
     */
    public SampledRateCounterConfig(int intervalSecs, int historySize, boolean isResetOnSample) {
        this(intervalSecs, historySize, isResetOnSample, 0, 0);
    }

    /**
     * Constructor accepting the interval time in seconds, history-size and
     * whether counters should reset on each sample or not. Also the initial
     * values for the numerator and the denominator
     * 
     * @param intervalSecs
     * @param historySize
     * @param isResetOnSample
     * @param initialNumeratorValue
     * @param initialDenominatorValue
     */
    public SampledRateCounterConfig(int intervalSecs, int historySize, boolean isResetOnSample, long initialNumeratorValue,
            long initialDenominatorValue) {
        super(intervalSecs, historySize, isResetOnSample, 0);
        this.initialNumeratorValue = initialNumeratorValue;
        this.initialDenominatorValue = initialDenominatorValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Counter createCounter() {
        SampledRateCounterImpl sampledRateCounter = new SampledRateCounterImpl(this);
        sampledRateCounter.setValue(initialNumeratorValue, initialDenominatorValue);
        return sampledRateCounter;
    }

}
