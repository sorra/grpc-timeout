package com.example;

import io.grpc.Context;
import io.grpc.Deadline;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** A global instance that manages server call timeouts. */
public class CancellableTimeoutManager {

    private final int timeout;
    private final TimeUnit unit;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Creates a manager. Please make it a singleton and remember to shut it down.
     *
     * @param timeout Configurable timeout threshold. A value less than 0 (e.g. 0 or -1) means not to
     *     check timeout.
     * @param unit The unit of the timeout.
     */
    public CancellableTimeoutManager(int timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
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
    public void intercept(Runnable invocation) {
        if (timeout <= 0) {
            invocation.run();
            return;
        }

        try (Context.CancellableContext withDeadline = Context.current()
                .withDeadline(Deadline.after(timeout, unit), scheduler)) {
            withDeadline.run(invocation);
        }
    }
}
