package com.rbac.service;

import com.rbac.exception.AccessDeniedException;
import com.rbac.exception.DuplicateResourceException;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.AttemptAnswer;
import com.rbac.model.AuditLog;
import com.rbac.model.Question;
import com.rbac.model.Quiz;
import com.rbac.model.QuizAttempt;
import com.rbac.model.QuizExtensionRequest;
import com.rbac.model.QuizReviewDto;
import com.rbac.repository.AttemptAnswerRepository;
import com.rbac.repository.QuestionRepository;
import com.rbac.repository.QuizAttemptRepository;
import com.rbac.repository.QuizExtensionRequestRepository;
import com.rbac.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service managing online quizzes, question banks, timed student attempts, and auto-grading.
 * Critical Security: Enforces time limits on server and ensures correct options are stripped prior to submission.
 */
@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final QuizExtensionRequestRepository extensionRequestRepository;
    private final AuditService auditService;

    public QuizService(QuizRepository quizRepository,
                       QuestionRepository questionRepository,
                       QuizAttemptRepository attemptRepository,
                       AttemptAnswerRepository answerRepository,
                       QuizExtensionRequestRepository extensionRequestRepository,
                       AuditService auditService) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.extensionRequestRepository = extensionRequestRepository;
        this.auditService = auditService;
    }

    public List<Quiz> listAllQuizzes() {
        return quizRepository.findAll();
    }

    public List<Quiz> listQuizzesByCreator(Long userId) {
        return quizRepository.findByCreatedBy(userId);
    }

    public Quiz getQuizById(Long quizId) {
        Quiz q = quizRepository.findById(quizId);
        if (q == null) {
            throw new ResourceNotFoundException("Quiz with ID " + quizId + " not found.");
        }
        return q;
    }

    public boolean hasAttempts(Long quizId) {
        return attemptRepository.countByQuizId(quizId) > 0;
    }

    @Transactional
    public Quiz createQuiz(Long courseId, Long createdBy, String title, int durationMinutes,
                           LocalDateTime startTime, LocalDateTime endTime) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0 minutes.");
        }
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        Quiz q = new Quiz();
        q.setCourseId(courseId);
        q.setCreatedBy(createdBy);
        q.setTitle(title.trim());
        q.setDurationMinutes(durationMinutes);
        q.setStartTime(startTime != null ? startTime : LocalDateTime.now());
        q.setEndTime(endTime != null ? endTime : LocalDateTime.now().plusDays(7));
        q.setIsPublished(false);

        Long id = quizRepository.save(q);
        q.setId(id);
        auditService.log(createdBy, "QUIZ_CREATE", "Created quiz '" + title + "' (ID: " + id + ")", AuditLog.Status.SUCCESS);
        return q;
    }

    @Transactional
    public void updateQuiz(Long id, Long courseId, String title, int durationMinutes,
                           LocalDateTime startTime, LocalDateTime endTime, Boolean isPublished, Long currentUserId) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0 minutes.");
        }
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        Quiz q = getQuizById(id);
        if (courseId != null) {
            q.setCourseId(courseId);
        }
        q.setTitle(title.trim());
        q.setDurationMinutes(durationMinutes);
        if (startTime != null) {
            q.setStartTime(startTime);
        }
        if (endTime != null) {
            q.setEndTime(endTime);
        }
        if (isPublished != null) {
            q.setIsPublished(isPublished);
        }

        quizRepository.update(q);
        auditService.log(currentUserId, "QUIZ_UPDATE", "Updated quiz '" + q.getTitle() + "' (ID: " + id + ")", AuditLog.Status.SUCCESS);
    }

    @Transactional
    public void setPublished(Long quizId, boolean published, Long currentUserId) {
        getQuizById(quizId);
        quizRepository.setPublished(quizId, published);
        auditService.log(currentUserId, "QUIZ_PUBLISH", "Set published=" + published + " on quiz ID " + quizId, AuditLog.Status.SUCCESS);
    }

    @Transactional
    public void deleteQuiz(Long quizId, Long currentUserId) {
        Quiz q = getQuizById(quizId);
        quizRepository.deleteById(quizId);
        auditService.log(currentUserId, "QUIZ_DELETE", "Deleted quiz '" + q.getTitle() + "' (ID: " + quizId + ")", AuditLog.Status.SUCCESS);
    }

    // Questions Management
    public List<Question> listQuestions(Long quizId) {
        return questionRepository.findByQuizId(quizId);
    }

    public Question getQuestionById(Long questionId) {
        Question q = questionRepository.findById(questionId);
        if (q == null) {
            throw new ResourceNotFoundException("Question not found.");
        }
        return q;
    }

    @Transactional
    public Question addQuestion(Long quizId, String questionText, String optA, String optB, String optC, String optD,
                                String correctOpt, Integer marks, Long currentUserId) {
        getQuizById(quizId);
        if (hasAttempts(quizId)) {
            throw new IllegalStateException("Cannot add questions: students have already attempted this quiz.");
        }
        if (correctOpt == null || !correctOpt.matches("[A-Da-d]")) {
            throw new IllegalArgumentException("Correct option must be A, B, C, or D.");
        }
        Question q = new Question();
        q.setQuizId(quizId);
        q.setQuestionText(questionText.trim());
        q.setOptionA(optA.trim());
        q.setOptionB(optB.trim());
        q.setOptionC(optC.trim());
        q.setOptionD(optD.trim());
        q.setCorrectOption(correctOpt.trim().toUpperCase());
        q.setMarks(marks != null && marks > 0 ? marks : 1);

        Long id = questionRepository.save(q);
        q.setId(id);
        auditService.log(currentUserId, "QUIZ_QUESTION_ADD", "Added question to quiz ID " + quizId, AuditLog.Status.SUCCESS);
        return q;
    }

    @Transactional
    public void updateQuestion(Long questionId, String questionText, String optA, String optB, String optC, String optD,
                               String correctOpt, Integer marks, Long currentUserId) {
        Question q = questionRepository.findById(questionId);
        if (q == null) {
            throw new ResourceNotFoundException("Question not found.");
        }
        if (hasAttempts(q.getQuizId())) {
            throw new IllegalStateException("Cannot edit questions: students have already attempted this quiz.");
        }
        if (correctOpt == null || !correctOpt.matches("[A-Da-d]")) {
            throw new IllegalArgumentException("Correct option must be A, B, C, or D.");
        }
        q.setQuestionText(questionText.trim());
        q.setOptionA(optA.trim());
        q.setOptionB(optB.trim());
        q.setOptionC(optC.trim());
        q.setOptionD(optD.trim());
        q.setCorrectOption(correctOpt.trim().toUpperCase());
        q.setMarks(marks != null && marks > 0 ? marks : 1);

        questionRepository.update(q);
        auditService.log(currentUserId, "QUIZ_QUESTION_UPDATE", "Updated question ID " + questionId, AuditLog.Status.SUCCESS);
    }

    @Transactional
    public void deleteQuestion(Long questionId, Long currentUserId) {
        Question q = questionRepository.findById(questionId);
        if (q != null) {
            if (hasAttempts(q.getQuizId())) {
                throw new IllegalStateException("Cannot delete questions: students have already attempted this quiz.");
            }
            questionRepository.deleteById(questionId);
            auditService.log(currentUserId, "QUIZ_QUESTION_DELETE", "Deleted question ID " + questionId, AuditLog.Status.SUCCESS);
        }
    }

    // Student Quiz Taking & Attempts
    public List<Quiz> listPublishedQuizzes() {
        return quizRepository.findPublished();
    }

    public List<Quiz> listPublishedQuizzesForStudent(Long studentId) {
        return quizRepository.findPublishedWithinWindowForStudent(studentId);
    }

    public QuizAttempt getStudentAttempt(Long quizId, Long studentId) {
        return attemptRepository.findByQuizAndStudent(quizId, studentId);
    }

    public List<Question> getQuestionsForStudentAttempt(Long quizId) {
        // Enforce: strip correct options!
        return questionRepository.findForStudentTakingQuiz(quizId);
    }

    @Transactional
    public QuizAttempt startAttempt(Long quizId, Long studentId, Long userId) {
        Quiz q = getQuizById(quizId);
        if (!q.isPublished()) {
            throw new AccessDeniedException("This quiz is not currently published.");
        }
        if (q.isExpired()) {
            throw new AccessDeniedException("This quiz has expired and cannot be attempted.");
        }
        if (q.isUpcoming()) {
            throw new AccessDeniedException("This quiz is upcoming and has not started yet.");
        }

        QuizAttempt existing = attemptRepository.findByQuizAndStudent(quizId, studentId);
        if (existing != null) {
            if (existing.isSubmitted()) {
                throw new DuplicateResourceException("You have already submitted an attempt for this quiz. Each quiz may only be taken once.");
            }
            return existing; // Resume active attempt
        }

        List<Question> questions = questionRepository.findByQuizId(quizId);
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("This quiz has no questions available to attempt.");
        }

        LocalDateTime now = LocalDateTime.now();
        int approvedExtra = extensionRequestRepository.sumApprovedMinutes(quizId, studentId);
        Long attemptId = attemptRepository.startAttempt(quizId, studentId, now, approvedExtra);
        QuizAttempt attempt = attemptRepository.findById(attemptId);
        auditService.log(userId, "QUIZ_START", "Started attempt ID " + attemptId + " on quiz '" + q.getTitle() + "'", AuditLog.Status.SUCCESS);
        return attempt;
    }

    @Transactional
    public QuizAttempt submitAttempt(Long quizId, Long studentId, Map<Long, String> selectedAnswers, Long userId) {
        Quiz q = getQuizById(quizId);
        QuizAttempt attempt = attemptRepository.findByQuizAndStudent(quizId, studentId);
        if (attempt == null) {
            throw new ResourceNotFoundException("No active quiz attempt found.");
        }
        if (attempt.isSubmitted()) {
            return attempt; // Already graded
        }

        LocalDateTime now = LocalDateTime.now();
        // Server-side time limit enforcement: allow a 1-minute buffer for network latency
        long baseMinutes = q.getDurationMinutes() != null && q.getDurationMinutes() > 0 ? q.getDurationMinutes() : 15;
        long extraMinutes = attempt.getExtraMinutes() != null ? attempt.getExtraMinutes() : 0;
        long minutesAllowed = baseMinutes + extraMinutes;
        Duration elapsed = Duration.between(attempt.getStartedAt(), now);
        if (elapsed.toSeconds() > (minutesAllowed * 60L + 60L)) {
            throw new AccessDeniedException("Time limit exceeded: Quiz attempt exceeded the allowed duration of " + minutesAllowed + " minutes.");
        }

        // Auto-grade against real questions
        List<Question> questions = questionRepository.findByQuizId(quizId);
        int totalScore = 0;

        for (Question question : questions) {
            String selected = selectedAnswers != null ? selectedAnswers.get(question.getId()) : null;
            if (selected != null) {
                selected = selected.trim().toUpperCase();
                answerRepository.save(attempt.getId(), question.getId(), selected);
                if (question.getCorrectOption() != null && question.getCorrectOption().equalsIgnoreCase(selected)) {
                    totalScore += question.getMarks();
                }
            } else {
                answerRepository.save(attempt.getId(), question.getId(), "");
            }
        }

        attemptRepository.submitAttempt(attempt.getId(), now, totalScore);
        attempt.setSubmittedAt(now);
        attempt.setScore(totalScore);

        auditService.log(userId, "QUIZ_SUBMIT", "Submitted quiz attempt ID " + attempt.getId() + " (Score: " + totalScore + ")", AuditLog.Status.SUCCESS);
        return attempt;
    }

    public List<QuizReviewDto> getAttemptReview(Long quizId, Long studentId) {
        QuizAttempt attempt = attemptRepository.findByQuizAndStudent(quizId, studentId);
        if (attempt == null || !attempt.isSubmitted()) {
            throw new AccessDeniedException("You cannot review a quiz attempt before submitting it.");
        }

        List<Question> questions = questionRepository.findByQuizId(quizId);
        Map<Long, String> answerMap = answerRepository.findAnswerMapByAttemptId(attempt.getId());

        List<QuizReviewDto> reviews = new ArrayList<>();
        for (Question q : questions) {
            String selected = answerMap.get(q.getId());
            boolean isCorrect = selected != null && q.getCorrectOption() != null &&
                    selected.trim().equalsIgnoreCase(q.getCorrectOption().trim());
            int marksEarned = isCorrect ? q.getMarks() : 0;
            reviews.add(new QuizReviewDto(q, selected, isCorrect, marksEarned));
        }
        return reviews;
    }

    public List<QuizAttempt> listQuizResults(Long quizId) {
        getQuizById(quizId);
        return attemptRepository.findByQuizId(quizId);
    }

    // ==========================================
    // Quiz Time Extension Requests
    // ==========================================
    public QuizExtensionRequest getExtensionRequestById(Long requestId) {
        return extensionRequestRepository.findById(requestId);
    }

    public List<QuizExtensionRequest> listExtensionRequestsForQuiz(Long quizId) {
        return extensionRequestRepository.findByQuizId(quizId);
    }

    public QuizExtensionRequest getStudentLatestExtensionRequest(Long quizId, Long studentId) {
        return extensionRequestRepository.findLatestByQuizAndStudent(quizId, studentId);
    }

    public QuizExtensionRequest getStudentPendingExtensionRequest(Long quizId, Long studentId) {
        return extensionRequestRepository.findPendingByQuizAndStudent(quizId, studentId);
    }

    @Transactional
    public QuizExtensionRequest createExtensionRequest(Long quizId, Long studentId, String reason, int requestedMinutes, Long userId) {
        Quiz q = getQuizById(quizId);
        if (!q.isPublished()) {
            throw new AccessDeniedException("Cannot request extension for an unpublished quiz.");
        }
        QuizAttempt attempt = attemptRepository.findByQuizAndStudent(quizId, studentId);
        if (attempt != null && attempt.isSubmitted()) {
            throw new AccessDeniedException("Cannot request extension: quiz attempt has already been submitted.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is required for time extension request.");
        }
        if (requestedMinutes < 1 || requestedMinutes > 30) {
            throw new IllegalArgumentException("Requested minutes must be between 1 and 30.");
        }

        QuizExtensionRequest pending = extensionRequestRepository.findPendingByQuizAndStudent(quizId, studentId);
        if (pending != null) {
            throw new DuplicateResourceException("You already have a pending extension request for this quiz.");
        }

        QuizExtensionRequest req = new QuizExtensionRequest();
        req.setQuizId(quizId);
        req.setStudentId(studentId);
        req.setReason(reason.trim());
        req.setRequestedMinutes(requestedMinutes);
        req.setStatus("PENDING");

        Long id = extensionRequestRepository.save(req);
        req.setId(id);
        auditService.log(userId, "QUIZ_EXTENSION_REQUEST", "Requested " + requestedMinutes + " min extension for quiz ID " + quizId, AuditLog.Status.SUCCESS);
        return req;
    }

    @Transactional
    public void approveExtensionRequest(Long requestId, Long decidedBy) {
        QuizExtensionRequest req = extensionRequestRepository.findById(requestId);
        if (req == null) {
            throw new ResourceNotFoundException("Extension request not found.");
        }
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalStateException("Request is already " + req.getStatus());
        }

        extensionRequestRepository.updateStatus(requestId, "APPROVED", decidedBy, LocalDateTime.now());

        // Add extra minutes to that student's own deadline only
        QuizAttempt attempt = attemptRepository.findByQuizAndStudent(req.getQuizId(), req.getStudentId());
        if (attempt != null) {
            attemptRepository.addExtraMinutes(attempt.getId(), req.getRequestedMinutes());
        }

        auditService.log(decidedBy, "QUIZ_EXTENSION_APPROVE", "Approved " + req.getRequestedMinutes() + " min extension for student ID " + req.getStudentId() + " on quiz ID " + req.getQuizId(), AuditLog.Status.SUCCESS);
    }

    @Transactional
    public void rejectExtensionRequest(Long requestId, Long decidedBy) {
        QuizExtensionRequest req = extensionRequestRepository.findById(requestId);
        if (req == null) {
            throw new ResourceNotFoundException("Extension request not found.");
        }
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalStateException("Request is already " + req.getStatus());
        }

        extensionRequestRepository.updateStatus(requestId, "REJECTED", decidedBy, LocalDateTime.now());
        auditService.log(decidedBy, "QUIZ_EXTENSION_REJECT", "Rejected extension request ID " + requestId + " for quiz ID " + req.getQuizId(), AuditLog.Status.SUCCESS);
    }

    // ==========================================
    // Student Notifications & Unattempted Quizzes
    // ==========================================
    public List<Quiz> getUnattemptedAvailableQuizzesForStudent(Long studentId) {
        return quizRepository.findUnattemptedAvailableQuizzesForStudent(studentId);
    }

    public int countUnattemptedAvailableQuizzesForStudent(Long studentId) {
        return quizRepository.countUnattemptedAvailableQuizzesForStudent(studentId);
    }

    public List<QuizExtensionRequest> getUnseenDecidedRequestsForStudent(Long studentId) {
        return extensionRequestRepository.findUnseenDecidedByStudentId(studentId);
    }

    @Transactional
    public void markExtensionUpdatesAsSeenForStudent(Long studentId) {
        if (studentId != null) {
            extensionRequestRepository.markAllSeenForStudent(studentId);
        }
    }

    @Transactional
    public void markExtensionRequestSeen(Long requestId, Long studentId) {
        if (requestId != null && studentId != null) {
            extensionRequestRepository.markSeenByIdAndStudent(requestId, studentId);
        }
    }
}
