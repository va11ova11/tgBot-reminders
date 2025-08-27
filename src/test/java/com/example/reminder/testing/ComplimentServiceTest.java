package com.example.reminder.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.reminder.application.ComplimentService;
import com.example.reminder.domain.ChatCompletionRequest;
import com.example.reminder.domain.Message;
import com.example.reminder.domain.entity.ComplimentEntry;
import com.example.reminder.domain.repository.ComplimentRepository;
import com.example.reminder.domain.repository.ChatHistoryRepository;
import com.example.reminder.domain.entity.ChatHistoryEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ComplimentServiceTest {

    private ComplimentService service;
    private RestTemplate rest;
    private ComplimentRepository complimentRepository;
    private ChatHistoryRepository historyRepository;

    @BeforeEach
    void setUp() {
        rest = mock(RestTemplate.class);
        complimentRepository = mock(ComplimentRepository.class);
        historyRepository = mock(ChatHistoryRepository.class);
        // простая инициализация через конструктор, подмените значения под свой проект
        service = new ComplimentService(rest, "https://api.aitunnel.ru/v1/", "KEY123", "gpt-3.5-turbo", complimentRepository, historyRepository);
    }

    @Test
    void testFetchComplimentForChat_success() throws Exception {
        Long chatId = 123L;
        when(complimentRepository.findTopByChatIdOrderByCreatedAtDesc(chatId))
                .thenReturn(Optional.empty());

        // мокируем ответ нейросети
        String body = "{\"choices\":[{\"message\":{\"content\":\"Красиво, ты умничка!\"}}]}";
        when(rest.exchange(eq("https://api.aitunnel.ru/v1/"), any(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        String result = service.fetchComplimentForChat(chatId);

        assertEquals("Красиво, ты умничка!", result);
        // проверяем, что запись сохранена
        verify(complimentRepository, times(1)).save(any(ComplimentEntry.class));
    }

    @Test
    void testFetchComplimentForChat_unauthorized() {
        Long chatId = 123L;
        when(complimentRepository.findTopByChatIdOrderByCreatedAtDesc(chatId))
                .thenReturn(Optional.empty());

        when(rest.exchange(eq("https://api.aitunnel.ru/v1/"), any(), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        String result = service.fetchComplimentForChat(chatId);
        assertNull(result);
    }
}