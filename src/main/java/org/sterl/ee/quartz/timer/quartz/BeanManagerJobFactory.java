package org.sterl.ee.quartz.timer.quartz;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.*;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * https://devsoap.com/injecting-cdi-managed-beans-into-quarz-jobs/
 * https://github.com/apache/deltaspike/blob/master/deltaspike/modules/scheduler/impl/src/main/java/org/apache/deltaspike/scheduler/impl/CdiAwareJobFactory.java
 * https://github.com/apache/deltaspike/blob/master/deltaspike/core/api/src/main/java/org/apache/deltaspike/core/api/provider/BeanProvider.java
 * <ul>
 *  <li>Breaks the idea of inverse of control
 *  <li>Somehow complex
 *  <li>Platform dependant
 * </ul>
 */
@Deprecated
@ApplicationScoped
public class BeanManagerJobFactory implements JobFactory {
    @Inject BeanManager beanManager;
    
    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        final Class<?> clazz = bundle.getJobDetail().getJobClass();
        final String name = bundle.getJobDetail().getKey().getName();
        final Optional<Bean<?>> bean = this.beanManager.getBeans(clazz, new AnnotationLiteral<Any>() {}).stream().findFirst(); // apply additional filter using the Job key
        if (bean.isEmpty()) throw new SchedulerException("No bean of type " + clazz + " with the name " + name + " found.");
        
        final CreationalContext<?> ctx = beanManager.createCreationalContext(bean.get());
        return (Job)beanManager.getReference(bean.get(), clazz, ctx);
    }
}
