package com.clarity_mantra.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "insight_cards")
public class InsightCard extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationSession conversation;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String quote;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String meaning;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reflectionPrompt;

    @Column(columnDefinition = "TEXT")
    private String shloka;
}
