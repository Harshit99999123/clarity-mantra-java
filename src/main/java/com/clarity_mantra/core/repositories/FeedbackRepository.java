package com.clarity_mantra.core.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clarity_mantra.core.entities.FeedbackSubmission;

public interface FeedbackRepository extends JpaRepository<FeedbackSubmission, Long> {
}
