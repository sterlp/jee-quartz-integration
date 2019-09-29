package org.sterl.ee.quartz.timer.api;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.sterl.ee.quartz.timer.timer.CdiTimer;
import org.sterl.ee.quartz.timer.timer.EjbTimer;
import org.sterl.ee.quartz.timer.timer.SlowNonConcurrentCdiTimer;

@RequestScoped
@Path("timer")
public class TimerResource {
    @Inject CdiTimer cdiTimer;
    @Inject SlowNonConcurrentCdiTimer clowCdiTimer;
    @Inject EjbTimer ejbTimer;
    
    @Inject Scheduler scheduler;

    @GET
    public Response ping(){
        return Response.ok(Json.createObjectBuilder()
                .add(cdiTimer.getClass().getName(), cdiTimer.getRunCount())
                .add(ejbTimer.getClass().getName(), ejbTimer.getRunCount())
                .add(clowCdiTimer.getClass().getName(), clowCdiTimer.getRunCount())
                .build())
            .build();
    }
    
    @POST
    @Path("/pause-timers")
    public Response pauseTimers() throws SchedulerException {
        scheduler.pauseJobs(GroupMatcher.jobGroupEquals("Cron Timer"));
        return Response.ok("Cron Timer paused").build();
    }
    @POST
    @Path("/resume-timers")
    public Response resumeTimers() throws SchedulerException {
        scheduler.resumeJobs(GroupMatcher.jobGroupEquals("Cron Timer"));
        return Response.ok("Cron Timer resumed").build();
    }
    
    /**
     * <ul>
     * <li>Job – Represents the actual job to be executed
     * <li>JobDetail – Conveys the detail properties of a given Job instance
     * <li>Trigger – Triggers are the mechanism by which Jobs are scheduled
     * </ul>
     * @param job
     * @param jobData
     * @param triggerCount
     * @param jobSleepTime
     * @param failTriggerCreation
     * @return status
     * @throws org.quartz.SchedulerException
     */
    @Transactional(Transactional.TxType.REQUIRED)
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response trigger(
            @FormParam("job") String job,
            @FormParam("jobData") String jobData,
            @FormParam("triggerCount") int triggerCount,
            @FormParam("jobSleepTime") long jobSleepTime,
            @FormParam("failTriggerCreation") String failTriggerCreation) throws SchedulerException {
        
        
        int count = 0;
        for (; count < triggerCount; ++count) {
            final JobDataMap data = new JobDataMap();
            data.put("jobData", jobData);
            data.put("jobSleepTime", jobSleepTime * 1000);
            data.put("jobIndex", count);
            scheduler.triggerJob(JobKey.jobKey(job, "JOB"), data);
        }
        
        if ("on".equalsIgnoreCase(failTriggerCreation)) {
            throw new RuntimeException("Failing creation of the trigger as requested! No running job should be in the log for this submit.");
        }
        return Response.ok("Triggered " + count + " times job " + job).build();
    }
}
