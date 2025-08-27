package com.example.reminder.domain.entity;

import jakarta.persistence.*;
import java.util.Objects;


@Entity
@Table(name = "chat_histories")
public class ChatHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    // JSON-строка, хранящая историю сообщений: [{"role":"user","content":"..."}...]
    @Column(name = "messages", columnDefinition = "TEXT")
    private String messages;

    public ChatHistoryEntity() {}

    public ChatHistoryEntity(Long chatId, String messages) {
        this.chatId = chatId;
        this.messages = messages;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public String getMessages() { return messages; }
    public void setMessages(String messages) { this.messages = messages; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatHistoryEntity)) return false;
        ChatHistoryEntity that = (ChatHistoryEntity) o;
        return Objects.equals(chatId, that.chatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId);
    }
}