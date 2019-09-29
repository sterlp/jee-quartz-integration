package org.sterl.ee.quartz.timer.timer;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import javax.enterprise.context.ApplicationScoped;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sterl.ee.quartz.common.SleepUtil;

/**
 * Just for demonstration a very slow timer class, which should log slower as the cron trigger.
 */
@DisallowConcurrentExecution
@ApplicationScoped
public class SlowNonConcurrentCdiTimer implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(SlowNonConcurrentCdiTimer.class);
    private final AtomicLong runCount = new AtomicLong(0L);
    
    public long getRunCount() {
        return runCount.get();
    }
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SleepUtil.sleep(20000);
        LOG.info("Hello from Slow CDI Timer {} {}...", runCount.incrementAndGet(), Instant.now());
    }
}