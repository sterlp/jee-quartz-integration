package org.sterl.ee.quartz.timer.timer;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sterl.ee.quartz.timer.logic.FooService;

@Stateless
@LocalBean // tells JEE to not mess around because we use an interface, this is not a remote EJB!
public class EjbTimer implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(EjbTimer.class);
    
    @Inject FooService fooService;
    private final AtomicLong runCount = new AtomicLong(0L);
    
    public long getRunCount() {
        return runCount.get();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.info("Hello from EJB Timer {} {} {}...", runCount.incrementAndGet(), fooService, Instant.now());
    }
}
