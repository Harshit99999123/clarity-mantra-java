package com.clarity_mantra.core.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.dtos.request.FeedbackDtos;
import com.clarity_mantra.core.enums.FeedbackType;
import com.clarity_mantra.core.exceptions.NotFoundException;
import com.clarity_mantra.core.exceptions.ValidationException;
import com.clarity_mantra.core.entities.ConversationSession;
import com.clarity_mantra.core.entities.FeedbackSubmission;
import com.clarity_mantra.core.entities.User;
import com.clarity_mantra.core.repositories.ConversationSessionRepository;
import com.clarity_mantra.core.repositories.FeedbackRepository;
import com.clarity_mantra.core.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ConversationSessionRepository conversationSessionRepository;

    @Transactional
    public FeedbackDtos.FeedbackResponse submit(Long userId, FeedbackDtos.FeedbackRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException(MessageConstants.USER_NOT_FOUND));
        FeedbackType type = request.type() == null ? FeedbackType.GENERAL : request.type();

        if (type == FeedbackType.SESSION && request.conversationId() == null) {
            throw new ValidationException(MessageConstants.SESSION_FEEDBACK_REQUIRES_CONVERSATION);
        }

        FeedbackSubmission submission = new FeedbackSubmission();
        submission.setUser(user);
        submission.setType(type);
        submission.setRating(request.rating());
        submission.setComment(request.comment());
        if (request.conversationId() != null) {
            ConversationSession session = conversationSessionRepository.findById(request.conversationId())
                    .orElseThrow(() -> new NotFoundException(MessageConstants.CONVERSATION_NOT_FOUND));
            if (!session.getUser().getId().equals(userId)) {
                throw new NotFoundException(MessageConstants.CONVERSATION_NOT_FOUND);
            }
            submission.setConversation(session);
        }

        feedbackRepository.save(submission);
        log.info("Stored {} feedback for user {}", submission.getType(), userId);
        return new FeedbackDtos.FeedbackResponse(
                submission.getId(),
                submission.getType(),
                submission.getRating(),
                submission.getComment(),
                submission.getConversation() == null ? null : submission.getConversation().getId(),
                submission.getCreatedAt());
    }
}
