package com.example.reminder.domain.repository;

import com.example.reminder.domain.entity.ReminderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {
    Optional<ReminderEntity> findByChatId(Long chatId);
}