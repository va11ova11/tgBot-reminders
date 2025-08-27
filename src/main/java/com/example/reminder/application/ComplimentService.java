package com.example.reminder.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import com.example.reminder.domain.ChatCompletionRequest;
import com.example.reminder.domain.Message;
import com.example.reminder.domain.entity.ComplimentEntry;
import com.example.reminder.domain.entity.ChatHistoryEntity;
import com.example.reminder.domain.repository.ComplimentRepository;
import com.example.reminder.domain.repository.ChatHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ComplimentService {
    private static final Logger logger = LoggerFactory.getLogger(ComplimentService.class);

    private final RestTemplate rest;
    private final String url;
    private final String model;
    private final ComplimentRepository complimentRepository;
    private final ChatHistoryRepository historyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    public ComplimentService(
            RestTemplate rest,
            @Value("${aitunnel.api.url}") String url,
            @Value("${aitunnel.api.key}") String apiKey,
            @Value("gpt-5-nano") String model,
            ComplimentRepository complimentRepository,
            ChatHistoryRepository historyRepository) {
        this.rest = rest;
        this.url = url;
        this.model = model;
        this.complimentRepository = complimentRepository;
        this.historyRepository = historyRepository;
        this.apiKey = apiKey;
    }

    // Получить уникальный комплимент для chatId, с сохранением в БД и истории
    public String fetchComplimentForChat(Long chatId) {
        String lastCompliment = complimentRepository.findTopByChatIdOrderByCreatedAtDesc(chatId)
                .map(ComplimentEntry::getText)
                .orElse(null);

        // Загрузить историю чатов для этого chatId
        List<Message> history = new ArrayList<>();
        Optional<ChatHistoryEntity> histOpt = historyRepository.findByChatId(chatId);
        if (histOpt.isPresent()) {
            String json = histOpt.get().getMessages();
            if (json != null && !json.isEmpty()) {
                try {
                    history = objectMapper.readValue(json, new TypeReference<List<Message>>() {});
                } catch (Exception e) {
                    logger.warn("ComplimentService: не смог распарсить историю чатов: {}", e.getMessage(), e);
                }
            }
        }

        // Промпт/сообщение к нейросети
        String prompt = "Напиши искренний комплимент для Веры, самой лучшей девушке в мире." +
                " Комплимент должен быть уникальным и без лишнего текста ";

        // Добавляем новый user-сообщение (history + новый prompt)
        List<Message> messagesForRequest = new ArrayList<>(history);
        messagesForRequest.add(new Message("user", prompt));

        ChatCompletionRequest request = new ChatCompletionRequest(model, messagesForRequest);

        String nextCompliment = postAndParse(request);

        if (nextCompliment != null && !nextCompliment.isBlank()) {
            // сохранить комплимент
            ComplimentEntry entry = new ComplimentEntry(chatId, nextCompliment);
            complimentRepository.save(entry);
            logger.info("ComplimentService: сохранён новый комплимент для чата {}", chatId);

            // обновить историю: history + (prompt) + (assistant: nextCompliment)
            messagesForRequest.add(new Message("assistant", nextCompliment));
            try {
                String updatedJson = objectMapper.writeValueAsString(messagesForRequest);
                ChatHistoryEntity hist = histOpt.orElseGet(() -> new ChatHistoryEntity(chatId, updatedJson));
                hist.setChatId(chatId);
                hist.setMessages(updatedJson);
                historyRepository.save(hist);
            } catch (Exception e) {
                logger.error("ComplimentService: ошибка сохранения истории чатов: {}", e.getMessage(), e);
            }

            return nextCompliment;
        }

        // fallback на последний известный комплимент
        if (lastCompliment != null && !lastCompliment.isBlank()) {
            logger.info("ComplimentService: возвращаю последний известный комплимент для чата {}", chatId);
            return lastCompliment;
        }

        return "Ты умничка!";
    }

    private String postAndParse(ChatCompletionRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(request, headers);
            var resp = rest.exchange(url, HttpMethod.POST, entity, String.class);

            if (resp.getBody() == null) return null;

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode content = choices.get(0).path("message").path("content");
                if (!content.isMissingNode()) {
                    return content.asText();
                }
                JsonNode text = choices.get(0).path("text");
                if (!text.isMissingNode()) {
                    return text.asText();
                }
            }
            return root.toString();
        } catch (Exception e) {
            logger.error("ComplimentService: ошибка получения комплимента: {}", e.getMessage(), e);
            return null;
        }
    }
}