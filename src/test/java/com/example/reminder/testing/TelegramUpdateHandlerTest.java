package com.example.reminder.testing;

import com.example.reminder.adapter.TelegramUpdateHandler;
import com.example.reminder.application.ReminderService;
import com.example.reminder.application.ComplimentService;
import com.example.reminder.application.MessageSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class TelegramUpdateHandlerTest {

    private ReminderService reminderService;
    private ComplimentService complimentService;
    private MessageSender messageSender;
    private TelegramUpdateHandler handler;

    @BeforeEach
    void setUp() {
        reminderService = mock(ReminderService.class);
        complimentService = mock(ComplimentService.class);
        messageSender = mock(MessageSender.class);

        handler = new TelegramUpdateHandler(reminderService, complimentService, messageSender);
    }

    @Test
    void testHandleStartUpdates() throws Exception {
        String json = "{ \"update_id\": 1, \"message\": { \"message_id\": 1, \"from\": {\"id\": 123, \"language_code\": \"ru\"}, \"chat\": {\"id\": 55555, \"type\": \"private\"}, \"text\": \"/start\" } }";

        ObjectMapper om = new ObjectMapper();
        JsonNode update = om.readTree(json);

        handler.handleUpdate(update);

        verify(reminderService, times(1)).registerReminderFromStart(eq(55555L), anyString());
        verify(messageSender, times(1)).sendMessage(eq(55555L), anyString());
    }

    @Test
    void testHandleYesCallback() throws Exception {
        String json = "{ \"update_id\": 2, \"callback_query\": { \"id\": \"abc\", \"data\": \"YES\", \"message\": { \"chat\": {\"id\": 55555} } } }";
        ObjectMapper om = new ObjectMapper();
        JsonNode update = om.readTree(json);

        handler.handleUpdate(update);

        // ожидаем вызова сервиса обработки YES, можно проверить через ReminderService
        verify(reminderService, times(1)).onYes(eq(55555L));
    }

    @Test
    void testHandleNoCallback() throws Exception {
        String json = "{ \"update_id\": 3, \"callback_query\": { \"id\": \"def\", \"data\": \"NO\", \"message\": { \"chat\": {\"id\": 55555} } } }";
        ObjectMapper om = new ObjectMapper();
        JsonNode update = om.readTree(json);

        handler.handleUpdate(update);

        verify(reminderService, times(1)).onNo(eq(55555L));
    }
}