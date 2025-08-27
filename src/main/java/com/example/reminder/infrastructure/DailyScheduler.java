package com.example.reminder.infrastructure;

import com.example.reminder.domain.Reminder;
import com.example.reminder.domain.scheduler.SchedulerStrategy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DailyScheduler implements SchedulerStrategy {
    private final ThreadPoolTaskScheduler scheduler;
    private final ConcurrentMap<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public DailyScheduler() {
        this.scheduler = new ThreadPoolTaskScheduler();
        this.scheduler.setPoolSize(5);
        this.scheduler.initialize();
    }

    @Override
    public void schedule(Reminder reminder, Runnable task) {
        // идемпотентность: отменяем старую задачу для этого chatId
        ScheduledFuture<?> previous = tasks.remove(reminder.getChatId());
        if (previous != null) previous.cancel(false);

        ZoneId zone = ZoneId.of(reminder.getTimeZone());
        String cron = String.format("0 0 8 * * *"); // fixed 08:00 по TZ по умолчанию
        CronTrigger trigger = new CronTrigger(cron, TimeZone.getTimeZone(zone));
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            task.run();
            // повторяем каждый день
            schedule(reminder, task);
        }, trigger);
        tasks.put(reminder.getChatId(), future);
    }

    @Override
    public ScheduledFuture<?> scheduleAfter(long delayMillis, Runnable task) {
        // задержка на delayMillis миллисекунд от текущего момента
        Date startTime = new Date(System.currentTimeMillis() + delayMillis);
        return scheduler.schedule(task, startTime);
    }

    @Override
    public void cancel(ScheduledFuture<?> future) {
        if (future != null) future.cancel(false);
    }

    @Override
    public void shutdown() {
        scheduler.shutdown();
    }
}