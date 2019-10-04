package org.quartz.core;

public class NullSampledStatisticsImpl implements SampledStatistics {
    public long getJobsCompletedMostRecentSample() {
        return 0;
    }

    public long getJobsExecutingMostRecentSample() {
        return 0;
    }

    public long getJobsScheduledMostRecentSample() {
        return 0;
    }

    public void shutdown() {
        // nothing to do
    }
}
