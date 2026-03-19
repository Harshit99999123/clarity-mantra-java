package com.clarity_mantra.core.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarity_mantra.core.entities.ConversationMessage;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findByConversation_IdOrderByCreatedAtAsc(Long conversationId);

    List<ConversationMessage> findTop10ByConversation_IdOrderByCreatedAtDesc(Long conversationId);
}
