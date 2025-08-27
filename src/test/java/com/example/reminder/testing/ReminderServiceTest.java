package com.example.reminder.testing;

import com.example.reminder.application.ComplimentService;
import com.example.reminder.application.ReminderService;
import com.example.reminder.domain.entity.ReminderEntity;
import com.example.reminder.domain.repository.ReminderRepository;
import com.example.reminder.domain.scheduler.SchedulerStrategy;
import com.example.reminder.application.MessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

public class ReminderServiceTest {

    private ReminderService service;
    private ReminderRepository repository;
    private MessageSender sender;
    private ComplimentService complimentService;
    private SchedulerStrategy scheduler;

    @BeforeEach
    void setUp() {
        repository = mock(ReminderRepository.class);
        sender = mock(MessageSender.class);
        complimentService = mock(ComplimentService.class);
        scheduler = mock(SchedulerStrategy.class);

        service = new ReminderService(repository, sender, scheduler, complimentService);
    }

    @Test
    void testOnYes_sendsCompliment() {
        long chatId = 42L;
        when(complimentService.fetchComplimentForChat(chatId)).thenReturn("Умничка, ты великолепна!");

        service.onYes(chatId);

        verify(complimentService, times(1)).fetchComplimentForChat(chatId);
        verify(sender, times(1)).sendMessage(chatId, "Умничка, ты великолепна!");
    }
}