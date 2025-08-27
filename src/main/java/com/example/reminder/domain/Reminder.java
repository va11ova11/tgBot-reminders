package com.example.reminder.domain;

public final class Reminder {
    private final long chatId;
    private final String timeZone;
    private final int hour;
    private final int minute;

    private Reminder(Builder b) {
        this.chatId = b.chatId;
        this.timeZone = b.timeZone;
        this.hour = b.hour;
        this.minute = b.minute;
    }

    public long getChatId() { return chatId; }
    public String getTimeZone() { return timeZone; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }

    public static Builder builder(long chatId) { return new Builder(chatId); }

    public static final class Builder {
        private final long chatId;
        private String timeZone = "UTC";
        private int hour = 8;
        private int minute = 0;

        private Builder(long chatId) { this.chatId = chatId; }

        public Builder timeZone(String timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public Builder time(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
            return this;
        }

        public Reminder build() {
            return new Reminder(this);
        }
    }
}