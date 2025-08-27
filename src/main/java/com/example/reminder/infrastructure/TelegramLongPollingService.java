package com.example.reminder.infrastructure;

import com.example.reminder.application.MessageSender;
import com.example.reminder.domain.repository.ReminderRepository;
import com.example.reminder.application.ReminderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.web.client.RestTemplate;

/**
 * Логика длинного опроса Telegram API (getUpdates).
 * Обрабатывает сообщения (/start) и callback_query.
 * Без вебхуков.
 */
@Component
public class TelegramLongPollingService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramLongPollingService.class);
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${telegram.bot.token}")
    private String token;

    private final ReminderService reminderService;
    private final com.example.reminder.application.MessageSender messageSender;
    private final com.example.reminder.infrastructure.TelegramHttpMessageSender httpSender; // для ack callback

    private volatile boolean running = true;
    private long offset = 0;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TelegramLongPollingService(ReminderService reminderService,
                                      com.example.reminder.application.MessageSender messageSender,
                                      TelegramHttpMessageSender httpSender) {
        this.reminderService = reminderService;
        this.messageSender = messageSender;
        this.httpSender = httpSender;
    }

    @PostConstruct
    public void start() {
        logger.info("TelegramLongPollingService: запуск длинного опроса Telegram API");
        executor.submit(this::run);
    }

    private void run() {
        try {
            while (running) {
                try {
                    String url = "https://api.telegram.org/bot" + token + "/getUpdates?offset=" + offset + "&timeout=60";
                    JsonNode response = rest.getForObject(url, JsonNode.class);
                    if (response != null && response.path("ok").asBoolean(false)) {
                        JsonNode results = response.path("result");
                        if (results.isArray()) {
                            for (JsonNode upd : results) {
                                long updId = upd.path("update_id").asLong();
                                if (updId >= offset) offset = updId + 1;
                                processUpdate(upd);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("TelegramLongPollingService: ошибка обработки обновления: {}", e.getMessage(), e);
                }
                // небольшая задержка, чтобы не перегружать API
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processUpdate(JsonNode update) {
        // Обработка обычного сообщения
        if (update.has("message")) {
            JsonNode msg = update.get("message");
            long chatId = msg.path("chat").path("id").asLong();
            String text = msg.path("text").asText(null);
            String languageCode = msg.path("from").path("language_code").asText("ru");

            String normalized = text;
            if (normalized != null && normalized.contains("@")) {
                int at = normalized.indexOf('@');
                normalized = normalized.substring(0, at);
            }

            if ("/start".equals(normalized)) {
                // Регистрируем напоминание от имени сервиса
                reminderService.registerReminderFromStart(chatId, "Europe/Moscow");
                messageSender.sendMessage(chatId,
                        "Привет, красотка! Я буду напоминать тебе пить таблетки от аллергии каждый день в 8:00 по Мск. ♥");
                logger.info("TelegramLongPollingService: /start зарегистрировано для чата {}", chatId);
            } else {
                logger.debug("TelegramLongPollingService: текст чата {}: {}", chatId, text);
            }
        }

        // Обработка callback_query
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

            httpSender.answerCallbackQuery(callbackQueryId);
            logger.info("TelegramLongPollingService: callback_query chatId={}, data={}", chatId, data);
        }
    }
}