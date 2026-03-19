package com.clarity_mantra.core.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarity_mantra.core.entities.InsightCard;

public interface InsightCardRepository extends JpaRepository<InsightCard, Long> {

    Optional<InsightCard> findTopByConversation_IdOrderByCreatedAtDesc(Long conversationId);
}
