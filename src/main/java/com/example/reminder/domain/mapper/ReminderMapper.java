package com.example.reminder.domain.mapper;

import com.example.reminder.domain.Reminder;
import com.example.reminder.domain.entity.ReminderEntity;

public class ReminderMapper {
    public static Reminder toDomain(ReminderEntity e) {
        if (e == null) return null;
        return Reminder.builder(e.getChatId())
                .timeZone(e.getTimeZone())
                .time(e.getHour(), e.getMinute())
                .build();
    }

    public static ReminderEntity toEntity(Reminder r) {
        ReminderEntity e = new ReminderEntity();
        e.setChatId(r.getChatId());
        e.setTimeZone(r.getTimeZone());
        e.setHour(r.getHour());
        e.setMinute(r.getMinute());
        return e;
    }
}