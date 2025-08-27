package com.example.reminder.domain.repository;

import com.example.reminder.domain.entity.ComplimentEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComplimentRepository extends JpaRepository<ComplimentEntry, Long> {
    // получить последний комплимент для чатId
    Optional<ComplimentEntry> findTopByChatIdOrderByCreatedAtDesc(Long chatId);
}
