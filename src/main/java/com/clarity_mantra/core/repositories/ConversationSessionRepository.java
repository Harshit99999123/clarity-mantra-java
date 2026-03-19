package com.clarity_mantra.core.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarity_mantra.core.entities.ConversationSession;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {

    List<ConversationSession> findByUser_IdOrderByUpdatedAtDesc(Long userId);

    Optional<ConversationSession> findByIdAndUser_Id(Long conversationId, Long userId);
}
