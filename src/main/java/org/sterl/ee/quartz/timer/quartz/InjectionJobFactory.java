package org.sterl.ee.quartz.timer.quartz;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * Just injection of all jobs // more less a what we do for a strategy pattern
 * a filter pattern.
 */
@ApplicationScoped
public class InjectionJobFactory implements JobFactory {
    
    @Inject @Any
    private Instance<Job> jobs;
    
    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        final Class<? extends Job> clazz = bundle.getJobDetail().getJobClass();
        final String name = bundle.getJobDetail().getKey().getName();
        final Instance<? extends Job> bean = jobs.select(clazz);
        
        if (bean.isUnsatisfied())throw new SchedulerException("No bean of type " + clazz + " with the name " + name + " found.");
        return bean.get();
    }
}
