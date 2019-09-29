package org.sterl.ee.quartz.timer.quartz;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;
import javax.enterprise.concurrent.ManagedExecutorService;
import org.quartz.spi.ThreadExecutor;
import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We have to bridge the ManagedExecutorService for quartz like the datasource.
 */
public class SimpleQuartzJeeThreadPool implements ThreadPool, ThreadExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleQuartzJeeThreadPool.class);

    private final ManagedExecutorService executorService;
    private final List<WeakReference<Future<?>>> tasks = Collections.synchronizedList(new ArrayList<>());

    /** the max pool size, could also be the max size of the custom executor services created */
    private final int poolSize;
    private boolean shutdown = false;

    public SimpleQuartzJeeThreadPool(ManagedExecutorService executorService, int poolSize) {
        this.executorService = executorService;
        this.poolSize = poolSize;
        Objects.requireNonNull(this.executorService, "ManagedExecutorService cannot be null.");
    }

    @Override
    public boolean runInThread(Runnable runnable) {
        if (shutdown) return false;

        try {
            tasks.add(new WeakReference<>(executorService.submit(runnable)));
            return true;
        } catch (RejectedExecutionException e) {
            LOG.info("runInThread was rejected, the pool capacitiy is exceeded! {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int blockForAvailableThreads() {
        if (shutdown) return -1;
        try {

            // check if we have a thread available again and block for quartz // and purge
            this.executorService.submit(this::purge).get();

            return poolSize - tasks.size();
        } catch (Exception e) {
            LOG.warn("blockForAvailableThreads failed to pruge queue!", e);
            return poolSize - tasks.size();
        }
    }
    /**
     * Removes all monitored jobs and returns the removed count.
     * @return the removed jobn count
     */
    public int purge() {
        int removed = 0;
        if (!tasks.isEmpty()) {   
            List<WeakReference<Future<?>>> doneJobs = tasks.stream().filter(e -> e.isEnqueued() || e.get() == null || e.get().isDone())
                    .collect(Collectors.toList());

            removed = doneJobs.size();
            this.tasks.removeAll(doneJobs);
            doneJobs.clear();
        }
        return removed;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        this.shutdown = true;
    }

    @Override
    public int getPoolSize() {
        return poolSize;
    }

    @Override
    public void setInstanceId(String schedInstId) {
    }

    @Override
    public void setInstanceName(String schedName) {
    }

    @Override
    public void execute(Thread thread) {
        this.executorService.execute(thread);
    }
}
