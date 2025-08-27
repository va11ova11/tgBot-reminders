package com.example.reminder.testing;

import com.example.reminder.domain.Reminder;
import com.example.reminder.domain.scheduler.SchedulerStrategy;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Delayed;

public class SynchronousScheduler implements SchedulerStrategy {

    @Override
    public void schedule(Reminder reminder, Runnable task) {
        // Немедленно выполняем задачу синхронно (для тестов)
        task.run();
    }

    @Override
    public ScheduledFuture<?> scheduleAfter(long delayMillis, Runnable task) {
        // Выполняем немедленно для тестов и возвращаем "фиктивный" Future
        task.run();
        return new ScheduledFuture<Object>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) { return true; }

            @Override
            public boolean isCancelled() { return false; }

            @Override
            public boolean isDone() { return true; }

            @Override
            public Object get() { return null; }

            @Override
            public Object get(long timeout, TimeUnit unit) { return null; }

            @Override
            public long getDelay(TimeUnit unit) { return 0; }

            @Override
            public int compareTo(Delayed o) {
                // простая реализация для тестов
                return 0;
            }
        };
    }

    @Override
    public void cancel(ScheduledFuture<?> future) {
        if (future != null) future.cancel(false);
    }

    @Override
    public void shutdown() {
        // ничего не нужно закрывать в тестовом варианте
    }
}