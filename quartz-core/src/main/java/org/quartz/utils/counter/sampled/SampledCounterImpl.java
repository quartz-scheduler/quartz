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

import java.util.TimerTask;

import org.quartz.utils.CircularLossyQueue;
import org.quartz.utils.counter.CounterImpl;

/**
 * An implementation of {@link SampledCounter}
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 * 
 */
public class SampledCounterImpl extends CounterImpl implements SampledCounter {
  
    private static final long serialVersionUID = -3605369302464131521L;
    
    private static final int MILLIS_PER_SEC = 1000;

    /**
     * The history of this counter
     */
    protected final CircularLossyQueue<TimeStampedCounterValue> history;

    /**
     * Should the counter reset on each sample?
     */
    protected final boolean resetOnSample;
    private final TimerTask samplerTask;
    private final long intervalMillis;

    /**
     * Constructor accepting a {@link SampledCounterConfig}
     * 
     * @param config
     */
    public SampledCounterImpl(SampledCounterConfig config) {
        super(config.getInitialValue());

        this.intervalMillis = config.getIntervalSecs() * MILLIS_PER_SEC;
        this.history = new CircularLossyQueue<TimeStampedCounterValue>(config.getHistorySize());
        this.resetOnSample = config.isResetOnSample();

        this.samplerTask = new TimerTask() {
            @Override
            public void run() {
                recordSample();
            }
        };

        recordSample();
    }

    /**
     * {@inheritDoc}
     */
    public TimeStampedCounterValue getMostRecentSample() {
        return this.history.peek();
    }

    /**
     * {@inheritDoc}
     */
    public TimeStampedCounterValue[] getAllSampleValues() {
        return this.history.toArray(new TimeStampedCounterValue[this.history.depth()]);
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        if (samplerTask != null) {
            samplerTask.cancel();
        }
    }

    /**
     * Returns the timer task for this sampled counter
     * 
     * @return the timer task for this sampled counter
     */
    public TimerTask getTimerTask() {
        return this.samplerTask;
    }

    /**
     * Returns the sampling thread interval in millis
     * 
     * @return the sampling thread interval in millis
     */
    public long getIntervalMillis() {
        return intervalMillis;
    }

    /**
     * {@inheritDoc}
     */
    void recordSample() {
        final long sample;
        if (resetOnSample) {
            sample = getAndReset();
        } else {
            sample = getValue();
        }

        final long now = System.currentTimeMillis();
        TimeStampedCounterValue timedSample = new TimeStampedCounterValue(now, sample);

        history.push(timedSample);
    }

    /**
     * {@inheritDoc}
     */
    public long getAndReset() {
        return getAndSet(0L);
    }
}
