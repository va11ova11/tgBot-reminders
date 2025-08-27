package com.example.reminder.domain.scheduler;

import com.example.reminder.domain.Reminder;
import java.util.concurrent.ScheduledFuture;

public interface SchedulerStrategy {
    void schedule(Reminder reminder, Runnable task);
    ScheduledFuture<?> scheduleAfter(long delayMillis, Runnable task);
    void cancel(ScheduledFuture<?> future);
    void shutdown();
}