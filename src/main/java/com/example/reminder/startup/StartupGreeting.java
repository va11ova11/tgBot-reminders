package com.example.reminder.startup;

import com.example.reminder.application.MessageSender;
import com.example.reminder.domain.entity.ReminderEntity;
import com.example.reminder.domain.repository.ReminderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@ConditionalOnProperty(name = "false", havingValue = "true", matchIfMissing = false)
public class StartupGreeting implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupGreeting.class);

    private final ReminderRepository repository;
    private final MessageSender sender;

    private static final String GREETING =
            "Привет, красотка! Я буду напоминать тебе пить таблетки от аллергии каждый день.♥";

    public StartupGreeting(ReminderRepository repository, MessageSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ReminderEntity> all = repository.findAll();
        logger.info("StartupGreeting: найдено {} зарегистрированных чатов", all.size());

        for (ReminderEntity e : all) {
            Long chatId = e.getChatId();
            if (chatId == null) continue;

            // вычисляем "сегодня" в часовом поясе чата
            ZoneId zone = ZoneId.of(e.getTimeZone());
            LocalDate todayInZone = LocalDate.now(zone);

            LocalDate lastGreeting = e.getLastGreetingDate();
            if (lastGreeting == null || !lastGreeting.equals(todayInZone)) {
                logger.info("StartupGreeting: отправляю приветствие чату {}", chatId);
                sender.sendMessage(chatId, GREETING);
                e.setLastGreetingDate(todayInZone);
                repository.save(e);
            }
        }
    }
}