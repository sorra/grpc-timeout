package com.example;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Context;
import io.grpc.Deadline;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** A global instance that manages server call timeouts. */
public class ContextTimeoutManager {

    private final int timeout;
    private final TimeUnit unit;

    private final Consumer<String> logFunction;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Creates a manager. Please make it a singleton and remember to shut it down.
     *
     * @param timeout Configurable timeout threshold. A value less than 0 (e.g. 0 or -1) means not to
     *     check timeout.
     * @param unit The unit of the timeout.
     * @param logFunction An optional function that can log (e.g. Logger::warn). Through this,
     *     we avoid depending on a specific logger library.
     */
    public ContextTimeoutManager(int timeout, TimeUnit unit, Consumer<String> logFunction) {
        this.timeout = timeout;
        this.unit = unit;
        this.logFunction = logFunction;
    }

    /** Please call shutdown() when the application exits. */
    public void shutdown() {
        scheduler.shutdownNow();
    }

    /**
     * Schedules a timeout and calls the RPC method invocation.
     * Invalidates the timeout if the invocation completes in time.
     *
     * @param invocation The RPC method invocation that processes a request.
     */
    public void withTimeout(Runnable invocation) {
        if (timeout <= 0 || scheduler.isShutdown()) {
            invocation.run();
            return;
        }

        try (Context.CancellableContext context = Context.current()
                .withDeadline(Deadline.after(timeout, unit), scheduler)) {
            Thread thread = Thread.currentThread();
            Context.CancellationListener cancelled = c -> {
                if (c.cancellationCause() == null) {
                    return;
                }
                thread.interrupt();
                if (logFunction != null) {
                    logFunction.accept(
                        "Interrupted RPC thread "
                            + thread.getName()
                            + " for timeout at "
                            + timeout
                            + " "
                            + unit);
                }
            };
            context.addListener(cancelled, MoreExecutors.directExecutor());
            context.run(invocation);
        }
    }
}
