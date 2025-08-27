package com.example.reminder.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "reminders")
public class ReminderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(name = "hour", nullable = false)
    private int hour;

    @Column(name = "minute", nullable = false)
    private int minute;

    @Column(name = "greeted", nullable = false)
    private boolean greeted = false;

    @Column(name = "last_catch_up_date")
    private LocalDate lastCatchUpDate;

    @Column(name = "last_greeting_date")
    private LocalDate lastGreetingDate; // дата последнего приветствия

    @Column(name = "follow_up_active", nullable = false)
    private boolean followUpActive = true;

    public boolean isGreeted() {
        return greeted;
    }
    public void setGreeted(boolean greeted) {
        this.greeted = greeted;
    }

    public boolean isFollowUpActive() {
        return followUpActive;
    }
    public void setFollowUpActive(boolean followUpActive) {
        this.followUpActive = followUpActive;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public boolean isGreetingSent() { return greeted; }
    public void setGreetingSent(boolean greeted) { this.greeted = greeted; }

    public LocalDate getLastCatchUpDate() { return lastCatchUpDate; }
    public void setLastCatchUpDate(LocalDate lastCatchUpDate) { this.lastCatchUpDate = lastCatchUpDate; }

    public LocalDate getLastGreetingDate() { return lastGreetingDate; }
    public void setLastGreetingDate(LocalDate lastGreetingDate) { this.lastGreetingDate = lastGreetingDate; }
}