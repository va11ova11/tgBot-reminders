package com.example.reminder.domain.repository;

import com.example.reminder.domain.entity.ChatHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatHistoryRepository extends JpaRepository<ChatHistoryEntity, Long> {

    Optional<ChatHistoryEntity> findByChatId(Long chatId);
}
