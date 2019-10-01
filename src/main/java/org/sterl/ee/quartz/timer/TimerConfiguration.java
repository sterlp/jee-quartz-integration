package org.sterl.ee.quartz.timer;

import java.sql.Connection;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.resource.spi.ConfigProperty;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.*;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JobStoreCMT;
import org.quartz.spi.ThreadExecutor;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sterl.ee.quartz.timer.timer.EjbTimer;
import org.sterl.ee.quartz.timer.quartz.InjectionJobFactory;
import org.sterl.ee.quartz.timer.quartz.SimpleConnectionProvider;
import org.sterl.ee.quartz.timer.quartz.SimpleQuartzJeeThreadPool;
import org.sterl.ee.quartz.timer.task.ConcurrentCdiTask;
import org.sterl.ee.quartz.timer.task.NonConccurentCdiTask;
import org.sterl.ee.quartz.timer.timer.CdiTimer;
import org.sterl.ee.quartz.timer.timer.SlowNonConcurrentCdiTimer;

/**
 * https://github.com/spring-projects/spring-framework/blob/master/spring-context-support/src/main/java/org/springframework/scheduling/quartz/LocalDataSourceJobStore.java
 */
@Startup
@Singleton
public class TimerConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(TimerConfiguration.class);
    
    private static final String QUARTZ_DS = "jdbc/quartz-datasource";
    @Resource(lookup = QUARTZ_DS)
    private DataSource quartzDataSource;
    
    private static final String APP_DS = "jdbc/app-datasource";
    @Resource(lookup = APP_DS)
    private DataSource appDataSource;
    
    @Inject InjectionJobFactory jobFactory;

    @Resource(name = "concurrent/quartz-executor")
    private ManagedExecutorService executorService;
    
    private Scheduler scheduler;
    
    static {
        System.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
    }
    
    @PostConstruct
    public void start() {
        setupQuartzSchema();
        startQuartz();
    }
    
    private void startQuartz() {
        try {
            // Get scheduler and start it
            final DirectSchedulerFactory schedulerFactory = DirectSchedulerFactory.getInstance();
            // provide the datasources to quartz
            DBConnectionManager.getInstance()
                    .addConnectionProvider(APP_DS, new SimpleConnectionProvider(appDataSource));
            DBConnectionManager.getInstance()
                    .addConnectionProvider(QUARTZ_DS, new SimpleConnectionProvider(quartzDataSource));
            
            JobStoreCMT jobStore = new JobStoreCMT();
            jobStore.setInstanceName("quartz-job-example-store");
            jobStore.setDataSource(APP_DS);
            jobStore.setNonManagedTXDataSource(QUARTZ_DS);
            // use the JEE executor service
            jobStore.setThreadExecutor(new ThreadExecutor() {
                @Override
                public void execute(Thread thread) {
                    executorService.execute(thread);
                }
                @Override
                public void initialize() {
                }
            });
            jobStore.setIsClustered(true);
            jobStore.setLockOnInsert(false);
            jobStore.setTablePrefix("QRTZ_");
            jobStore.setDriverDelegateClass("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
            
            // instance id should differ between the nodes
            System.getProperties().entrySet().forEach(e -> LOG.info("{} --> {}", e.getKey(), e.getValue()));
            String quartzNodeId = System.getProperty("quartz.node.id");
            if (null == quartzNodeId) {
                LOG.info("'quartz.node.id' not set, will generate unique id, provide if as system argument.");
                quartzNodeId = UUID.randomUUID().toString().substring(0, 4);
            }
            schedulerFactory.createScheduler("JEE-QUARTZ", "JEE-QUARTZ-ID-" + quartzNodeId, 
                    new SimpleQuartzJeeThreadPool(executorService, 20), jobStore);
            
            scheduler = schedulerFactory.getScheduler("JEE-QUARTZ");

            // Use the CDI managed job factory
            scheduler.setJobFactory(jobFactory);

            // Start scheduler
            scheduler.startDelayed(10);
            scheduler.start();

            LOG.info("Quartz created, delayed start ...");
            
            // we could or even should maybe just inject here all timers too.
            // just for better readability
            scheduleClass(scheduler, CdiTimer.class, "0/10 * * * * ?");
            scheduleClass(scheduler, EjbTimer.class, "0/10 * * * * ?");
            scheduleClass(scheduler, SlowNonConcurrentCdiTimer.class, "0/10 * * * * ?");
            
            // add simple job classes, triggered manually
            scheduler.addJob(JobBuilder.newJob(ConcurrentCdiTask.class).storeDurably(true)
                    .withIdentity(ConcurrentCdiTask.class.getSimpleName(), "JOB").build(), true);
            scheduler.addJob(JobBuilder.newJob(NonConccurentCdiTask.class).storeDurably(true)
                    .withIdentity(NonConccurentCdiTask.class.getSimpleName(), "JOB").build(), true);

        } catch (Exception e) {
            throw new RuntimeException("Failed to configure and start quartz.", e);
        }
    }
    
    /**
     * https://github.com/quartz-scheduler/quartz/wiki/How-to-Setup-Databases#using-liquibase-tool
     * https://github.com/quartz-scheduler/quartz/blob/master/quartz-core/src/main/resources/org/quartz/impl/jdbcjobstore/liquibase.quartz.init.xml
     */
    private void setupQuartzSchema() {
        try (Connection c = appDataSource.getConnection()) {
            Liquibase l = new Liquibase(
                    "/liquibase.quartz.init.xml", 
                    new ClassLoaderResourceAccessor(getClass().getClassLoader()),
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c)));
            l.update("QUARTZ-SCHEMA");
        } catch (Exception e) {
            throw new RuntimeException("Failed to init Quartz Schema.", e);
        }
    }
    
    @Produces
    @ApplicationScoped
    public Scheduler produceScheduler() {
        return this.scheduler;
    }
    
    private static void scheduleClass(Scheduler scheduler, Class<? extends Job> clazz, String cron) throws SchedulerException {
        // Create a QuarzJob to run
        final JobDetail job = JobBuilder.newJob(clazz)
                .storeDurably(true)
                .withIdentity(clazz.getSimpleName(), "Cron Timer")
                .build();
        
        // Register Job for the cron Trigger
        if (!scheduler.checkExists(job.getKey())) {
            scheduler.addJob(job, true);
        }
        
        // Create a Trigger to trigger the job for the given cron string
        Trigger trigger = TriggerBuilder.newTrigger()
                         .withIdentity(clazz.getSimpleName() + "-cron-" + cron, "Cron Timer")
                         .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                         .forJob(job)			
                         .build();
        if (!scheduler.checkExists(trigger.getKey())) {
            scheduler.scheduleJob(trigger);
        }
    }
    
    @PreDestroy
    public void stop() {
        try {
            this.scheduler.shutdown(true);
            LOG.info("Quartz stopped ...");
        } catch (SchedulerException e) {
            LOG.info("Failed to stop Quartz...", e);
        }
    }
}
