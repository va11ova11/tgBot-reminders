package com.example.reminder.infrastructure;

import com.example.reminder.application.MessageSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TelegramHttpMessageSender implements MessageSender {

    private static final Logger logger = LoggerFactory.getLogger(TelegramHttpMessageSender.class);

    private final RestTemplate rest = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String token;

    @Override
    public void sendMessage(long chatId, String text) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", text);
        post(payload);
        logger.info("TelegramHttpMessageSender: отправлено сообщение в чат {}: {}", chatId, text);
    }

    @Override
    public void sendMessageWithInline(long chatId, String text) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", text);
        payload.put("reply_markup", Map.of(
                "inline_keyboard", new Object[][]{
                        { Map.of("text","Да","callback_data","YES"),
                                Map.of("text","Нет","callback_data","NO")}
                }
        ));

        post(payload);
        logger.debug("TelegramHttpMessageSender: отправлено сообщение с inline кнопками в чат {}: {}", chatId, text);
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId) {
        String url = "https://api.telegram.org/bot" + token + "/answerCallbackQuery";
        Map<String, String> payload = Map.of("callback_query_id", callbackQueryId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);
            rest.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            logger.error("Callback answer error: {}", e.getMessage(), e);
        }
    }

    private void post(Map<String, Object> payload) {
        try {
            String url = "https://api.telegram.org/bot" + token + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            rest.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            logger.error("Telegram send error: {}", e.getMessage(), e);
        }
    }
}