/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
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

package org.quartz.core;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.spi.TimeBroker;

/*
 * Test helper TimeBroker implementation used to verify that Quartz
 * uses the configured time source instead of System.currentTimeMillis().
 * author: Thanos Tsiamis https://github.com/ThanosTsiamis
 */
public class FakeTimeBroker implements TimeBroker {

    private static final AtomicLong NOW = new AtomicLong();

    public static void setNow(long millis) {
        NOW.set(millis);
    }

    public static long currentTimeMillis() {
        return NOW.get();
    }

    @Override
    public Date getCurrentTime() throws SchedulerException {
        return new Date(NOW.get());
    }

    @Override
    public void initialize() throws SchedulerConfigException {
        // no-op for tests
    }

    @Override
    public void shutdown() {
        // no-op for tests
    }
}