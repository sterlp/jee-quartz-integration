package org.sterl.ee.quartz.timer.task;

import javax.enterprise.context.ApplicationScoped;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sterl.ee.quartz.common.SleepUtil;

@DisallowConcurrentExecution
@ApplicationScoped
public class NonConccurentCdiTask implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(NonConccurentCdiTask.class);
    
    private final String name = this.getClass().getSimpleName();
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        final JobDataMap jobDataMap = context.getMergedJobDataMap();
        LOG.info("{} with data {} and index {} started ...", name, jobDataMap.getString("jobData"), jobDataMap.get("jobIndex"));
        SleepUtil.sleep(jobDataMap.getLong("jobSleepTime"));
        LOG.info(".. {} with data {} index {} finished.", name, jobDataMap.get("jobData"), jobDataMap.get("jobIndex"));
    }
}
