package com.example.reminder.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.reminder.application.ReminderService;
import com.example.reminder.application.ComplimentService;
import com.example.reminder.application.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelegramUpdateHandler {

    private static final Logger logger = LoggerFactory.getLogger(TelegramUpdateHandler.class);

    private final ReminderService reminderService;
    private final ComplimentService complimentService;
    private final MessageSender messageSender;

    public TelegramUpdateHandler(ReminderService reminderService,
                                 ComplimentService complimentService,
                                 MessageSender messageSender) {
        this.reminderService = reminderService;
        this.complimentService = complimentService;
        this.messageSender = messageSender;
    }

    public void handleUpdate(JsonNode update) {
        // Обработка обычного сообщения
        if (update.has("message")) {
            JsonNode msg = update.get("message");
            long chatId = msg.path("chat").path("id").asLong();
            String text = msg.path("text").asText(null);

            if (text != null && ("/start".equals(text) || text.startsWith("/start@"))) {
                // В LongPolling версии можно фиксировать TZ по умолчанию
                reminderService.registerReminderFromStart(chatId, "Europe/Moscow");
                messageSender.sendMessage(chatId, "Котенок, сегодня в 8:00 напомню тебе выпить таблетку");
                logger.info("TelegramUpdateHandler: /start обработано для чата {}", chatId);
            } else {
                logger.debug("TelegramUpdateHandler: сообщение чата {}: {}", chatId, text);
            }
        }

        // Обработка callback_query (кнопок Да/Нет)
        if (update.has("callback_query")) {
            JsonNode cb = update.get("callback_query");
            String data = cb.path("data").asText();
            String callbackQueryId = cb.path("id").asText();
            long chatId = cb.path("message").path("chat").path("id").asLong();

            if ("YES".equals(data)) {
                reminderService.onYes(chatId);
            } else if ("NO".equals(data)) {
                reminderService.onNo(chatId);
            }

            // ack callback_query можно реализовать здесь, если нужно
            logger.info("TelegramUpdateHandler: callback_query chatId={}, data={}", chatId, data);
        }
    }
}
