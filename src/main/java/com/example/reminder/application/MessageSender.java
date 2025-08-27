package com.example.reminder.application;

public interface MessageSender {
    void sendMessage(long chatId, String text);
    void sendMessageWithInline(long chatId, String text);
    void answerCallbackQuery(String callbackQueryId);
}