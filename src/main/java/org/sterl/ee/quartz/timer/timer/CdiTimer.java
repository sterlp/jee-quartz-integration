package org.sterl.ee.quartz.timer.timer;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sterl.ee.quartz.timer.logic.FooService;

@ApplicationScoped // timer don't have a scope or web request ...
public class CdiTimer implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(CdiTimer.class);

    @Inject FooService fooService;
    private final AtomicLong runCount = new AtomicLong(0L);
    
    public long getRunCount() {
        return runCount.get();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.info("Hello from CDI Timer {} {} {}...", runCount.incrementAndGet(), fooService, Instant.now());
    }
}
