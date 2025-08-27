package com.example.reminder.testing;

import com.example.reminder.domain.entity.ReminderEntity;
import com.example.reminder.domain.repository.ReminderRepository;
import com.example.reminder.domain.scheduler.SchedulerStrategy;
import com.example.reminder.application.ReminderService;
import com.example.reminder.domain.mapper.ReminderMapper;
import com.example.reminder.domain.entity.ReminderEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class RescheduleAllTest {

    private ReminderService service;
    private ReminderRepository repository;
    private SchedulerStrategy scheduler;
    private com.example.reminder.application.ComplimentService complimentService; // если нужен

    @BeforeEach
    void setUp() {
        repository = mock(ReminderRepository.class);
        SchedulerStrategy scheduler = mock(SchedulerStrategy.class);
        com.example.reminder.application.ComplimentService complimentService = mock(com.example.reminder.application.ComplimentService.class);
        // передайте необходимые зависимости в конструктор вашего ReminderService
        // service = new ReminderService(repository, sender, scheduler, complimentService);
    }

    @Test
    void testRescheduleAll_triggersSchedules() {
        // Пример минимальный: два чата
        ReminderEntity r1 = new ReminderEntity();
        r1.setChatId(1L);
        r1.setTimeZone("Europe/Moscow");
        r1.setHour(8);
        r1.setMinute(0);

        ReminderEntity r2 = new ReminderEntity();
        r2.setChatId(2L);
        r2.setTimeZone("Europe/Moscow");
        r2.setHour(8);
        r2.setMinute(0);

        when(repository.findAll()).thenReturn(Arrays.asList(r1, r2));
        // заманиваем простой spy/behavior для scheduler
        SchedulerStrategy sched = mock(SchedulerStrategy.class);

        // вызвать rescheduleAll
        // service.rescheduleAll();

        // verify scheduler.schedule вызван 2 раза
        verify(sched, times(2)).schedule(any(), any());
    }
}