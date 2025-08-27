package com.example.reminder.application;

import com.example.reminder.domain.Reminder;
import com.example.reminder.domain.entity.ReminderEntity;
import com.example.reminder.domain.mapper.ReminderMapper;
import com.example.reminder.domain.repository.ReminderRepository;
import com.example.reminder.domain.scheduler.SchedulerStrategy;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class ReminderService {
    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderRepository repository;
    private final MessageSender sender;
    private final SchedulerStrategy scheduler;
    private final ComplimentService complimentService;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> followUps = new ConcurrentHashMap<>();

    public ReminderService(ReminderRepository repository,
                           MessageSender sender,
                           SchedulerStrategy scheduler,
                           ComplimentService complimentService) {
        this.repository = repository;
        this.sender = sender;
        this.scheduler = scheduler;
        this.complimentService = complimentService;
    }

    // Регистрация напоминания при /start (upsert) — цепочка после отправки основного напоминания
    public void registerReminderFromStart(long chatId, String timeZone) {
        Reminder reminder = Reminder.builder(chatId)
                .timeZone(timeZone)
                .time(8, 0)
                .build();

        Optional<ReminderEntity> existingOpt = repository.findByChatId(chatId);
        ReminderEntity entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setTimeZone(reminder.getTimeZone());
            entity.setHour(reminder.getHour());
            entity.setMinute(reminder.getMinute());
            repository.save(entity);
            logger.info("ReminderService: обновлена запись напоминания для чата {}", chatId);
        } else {
            entity = ReminderMapper.toEntity(reminder);
            repository.save(entity);
            logger.info("ReminderService: создано напоминание для чата {}", chatId);
        }

        // Расписание ежедневного напоминания
        scheduler.schedule(reminder, () -> sender.sendMessage(chatId, "Котёнок, пора выпить таблетку.♥"));

        // Новая логика: если это новое напоминание и сейчас пропущено сегодня — отправить немедленно
        if (!existingOpt.isPresent()) {
            ZoneId zone = ZoneId.of(timeZone);
            ZonedDateTime now = ZonedDateTime.now(zone);
            ZonedDateTime todayRun = now.withHour(8).withMinute(0).withSecond(0).withNano(0);

            if (now.isAfter(todayRun)) {
                // пропущено сегодня, отправляем напоминание прямо сейчас
                sender.sendMessage(chatId, "Котёнок, пора выпить таблетку.♥");

                // follow-up через 5 минут
                ScheduledFuture<?> fu = scheduler.scheduleAfter(60_000L, () ->
                        sender.sendMessageWithInline(chatId, "Ты выпила таблетку?")
                );
                followUps.put(chatId, fu);
                logger.info("ReminderService: пропущено сегодня на старте — отправлен catch-up для чата {}", chatId);

                // пометка пропущенного на сегодня
                ReminderEntity updated = entity;
                updated.setLastGreetingDate(null); // оставляем приветствие отдельно
                updated.setLastGreetingDate(LocalDate.now(zone)); // если хотите, можно отдельно хранить lastCatchUp
                repository.save(updated);
            }
        }
    }

    public List<ReminderEntity> listReminders() {
        logger.debug("ReminderService: запрос списка напоминаний");
        return repository.findAll();
    }

    public void deleteReminder(Long id) {
        repository.deleteById(id);
        logger.info("ReminderService: удалено напоминание с id={}", id);
    }

    public void onYes(long chatId) {
        // Получаем комплимент через ComplimentService
        String compliment = complimentService.fetchComplimentForChat(chatId);
        if (compliment == null || compliment.isEmpty()) {
            compliment = "Умничка!!!"; // запасной вариант
        }

        logger.info("ReminderService: чат {} Ответ YES -> {}", chatId, compliment);
        sender.sendMessage(chatId, compliment);

        // Остановить цикл Да/Нет для этого чата
        repository.findByChatId(chatId).ifPresent(e -> {
            e.setFollowUpActive(false); // отключаем повторные follow-up после YES
            repository.save(e);
        });

        // Отменяем текущую запланированную follow-up
        ScheduledFuture<?> f = followUps.remove(chatId);
        if (f != null) scheduler.cancel(f);

        // Сообщение о следующем напоминании
        sender.sendMessage(chatId, "Следующее напоминание придёт завтра в 8:00♥");
    }

    public void onNo(long chatId) {
        logger.info("ReminderService: чат {} Ответ NO -> напоминание продолжается", chatId);

        // Проверяем, активно ли продолжение цикла
        boolean active = repository.findByChatId(chatId)
                .map(ReminderEntity::isFollowUpActive)
                .orElse(false);

        if (!active) {
            logger.info("ReminderService: цикл Да/Нет отключен для чата {}", chatId);
            return;
        }

        sender.sendMessage(chatId, "давай скорее у тебя есть 1 минута!");
        ScheduledFuture<?> f = scheduler.scheduleAfter(60_000L, () ->
                sender.sendMessageWithInline(chatId, "Ты выпила таблетку?")
        );
        followUps.put(chatId, f);
    }

    @PostConstruct
    public void rescheduleAll() {
        logger.info("ReminderService: перезапуск — восстанавливаю расписания и проверяю пропущенные сегодня");
        List<ReminderEntity> all = repository.findAll();
        LocalDate today = LocalDate.now();

        for (ReminderEntity e : all) {
            Long chatId = e.getChatId();
            if (chatId == null) continue;

            ZoneId zone = ZoneId.of(e.getTimeZone());
            ZonedDateTime now = ZonedDateTime.now(zone);
            ZonedDateTime todayRun = now.withHour(e.getHour())
                    .withMinute(e.getMinute())
                    .withSecond(0)
                    .withNano(0);

            boolean missedToday = now.isAfter(todayRun);
            LocalDate lastCatchUp = e.getLastGreetingDate(); // или ваш аналог

            // Пропущено сегодня и catch-up ещё не был сделан сегодня
            if (missedToday && (e.getLastGreetingDate() == null || !e.getLastGreetingDate().equals(today))) {
                // Сделать catch-up
                logger.info("ReminderService: пропущено сегодня для чата {} — выполняем catch-up сейчас", chatId);

                // базовое напоминание
                scheduler.scheduleAfter(0, () -> sender.sendMessage(chatId, "Котёнок, пора выпить таблетку."));

                // follow-up через 5 минут
                ScheduledFuture<?> fu = scheduler.scheduleAfter(60_000L, () ->
                        sender.sendMessageWithInline(chatId, "Ты выпила таблетку?")
                );
                followUps.put(chatId, fu);

                // помечаем сегодняшний catch-up
                e.setLastGreetingDate(today);
                // еcли нужно — отключаем повторные catch-up на сегодня
                // e.setFollowUpActive(false);
                repository.save(e);
            }

            // Планируем ежедневное напоминание (если follow-up cycle активен, он будет продолжать работать)
            Reminder r = ReminderMapper.toDomain(e);
            if (e.isFollowUpActive()) {
                scheduler.schedule(r, () -> sender.sendMessage(chatId, "Пора выпить таблетку."));
            } else {
                // если цикл выключен, не планируем повторно
            }
        }
    }
}