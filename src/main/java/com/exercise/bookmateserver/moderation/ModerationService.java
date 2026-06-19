package com.exercise.bookmateserver.moderation;

import com.exercise.bookmateserver.book.BookEntity;
import com.exercise.bookmateserver.book.BookRepository;
import com.exercise.bookmateserver.review.ReviewEntity;
import com.exercise.bookmateserver.review.ReviewRepository;
import com.exercise.bookmateserver.social.guestbook.GuestbookEntity;
import com.exercise.bookmateserver.social.guestbook.GuestbookRepository;
import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ModerationService {

    private static final int GLOBAL_HIDE_REPORT_THRESHOLD = 3;
    private static final Collection<ModerationReportStatus> ACTIVE_REPORT_STATUSES = List.of(
            ModerationReportStatus.PENDING,
            ModerationReportStatus.REVIEWED,
            ModerationReportStatus.RESOLVED
    );

    private final ModerationReportRepository reportRepository;
    private final UserBlockRepository blockRepository;
    private final UserRepository userRepository;
    private final GuestbookRepository guestbookRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final ContentModerationPolicy contentModerationPolicy;
    private final ObjectMapper objectMapper;

    public ModerationService(
            ModerationReportRepository reportRepository,
            UserBlockRepository blockRepository,
            UserRepository userRepository,
            GuestbookRepository guestbookRepository,
            BookRepository bookRepository,
            ReviewRepository reviewRepository,
            ContentModerationPolicy contentModerationPolicy,
            ObjectMapper objectMapper
    ) {
        this.reportRepository = reportRepository;
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.guestbookRepository = guestbookRepository;
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
        this.contentModerationPolicy = contentModerationPolicy;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ModerationReportEnvelope submitReport(UserEntity reporter, ModerationReportCreateRequest request) {
        String targetId = request.targetId().trim();
        UUID targetUserId = resolveTargetUserId(request);

        if (targetUserId != null && targetUserId.equals(reporter.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자신의 콘텐츠는 신고할 수 없습니다.");
        }

        if (targetUserId != null && !userRepository.existsById(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "신고 대상 사용자를 찾을 수 없습니다.");
        }

        return reportRepository
                .findFirstByReporterUserIdAndTargetTypeAndTargetIdAndStatusInOrderByCreatedAtDesc(
                        reporter.getId(),
                        request.targetType(),
                        targetId,
                        ACTIVE_REPORT_STATUSES
                )
                .map(existingReport -> createReportEnvelope(existingReport, true))
                .orElseGet(() -> {
                    ModerationReportEntity report = new ModerationReportEntity(
                            reporter.getId(),
                            request.targetType(),
                            targetId,
                            targetUserId,
                            request.reason(),
                            request.detail(),
                            serializeSnapshot(request.targetSnapshot())
                    );

                    ModerationReportEntity savedReport = reportRepository.save(report);
                    return createReportEnvelope(savedReport, true);
                });
    }

    @Transactional
    public UserBlockResponse blockUser(UserEntity blocker, BlockUserRequest request) {
        UUID blockedUserId = request.blockedUserId();
        if (blocker.getId().equals(blockedUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신은 차단할 수 없습니다.");
        }

        userRepository.findById(blockedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "차단할 사용자를 찾을 수 없습니다."));

        return blockRepository.findByBlockerUserIdAndBlockedUserId(blocker.getId(), blockedUserId)
                .map(UserBlockResponse::from)
                .orElseGet(() -> UserBlockResponse.from(blockRepository.save(
                        new UserBlockEntity(blocker.getId(), blockedUserId)
                )));
    }

    @Transactional
    public void unblockUser(UserEntity blocker, UUID blockedUserId) {
        blockRepository.deleteByBlockerUserIdAndBlockedUserId(blocker.getId(), blockedUserId);
    }

    public List<UserBlockResponse> findBlockedUsers(UserEntity blocker) {
        return blockRepository.findAllByBlockerUserIdOrderByCreatedAtDesc(blocker.getId())
                .stream()
                .map(UserBlockResponse::from)
                .toList();
    }

    public ContentCheckResponse checkContent(ContentCheckRequest request) {
        return contentModerationPolicy.check(request.content(), request.context());
    }

    public void validateContentAllowed(String content, ContentModerationContext context) {
        contentModerationPolicy.validateAllowed(content, context);
    }

    public boolean hasBlockBetween(UUID firstUserId, UUID secondUserId) {
        if (firstUserId == null || secondUserId == null || firstUserId.equals(secondUserId)) {
            return false;
        }

        return blockRepository.existsBlockBetween(firstUserId, secondUserId);
    }

    public void ensureNoBlockBetween(UUID firstUserId, UUID secondUserId) {
        if (hasBlockBetween(firstUserId, secondUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "차단 관계가 있어 접근할 수 없습니다.");
        }
    }

    public boolean shouldHideTargetForUser(UUID userId, ModerationTargetType targetType, String targetId) {
        if (userId == null || targetType == null || targetId == null || targetId.isBlank()) {
            return false;
        }

        String normalizedTargetId = targetId.trim();
        return reportRepository.existsByReporterUserIdAndTargetTypeAndTargetIdAndStatusIn(
                userId,
                targetType,
                normalizedTargetId,
                ACTIVE_REPORT_STATUSES
        ) || shouldHideTargetGlobally(targetType, normalizedTargetId);
    }

    public boolean shouldHideTargetGlobally(ModerationTargetType targetType, String targetId) {
        return reportRepository.countByTargetTypeAndTargetIdAndStatusIn(
                targetType,
                targetId,
                ACTIVE_REPORT_STATUSES
        ) >= GLOBAL_HIDE_REPORT_THRESHOLD;
    }

    public List<UUID> findBlockedUserIds(UserEntity blocker) {
        return blockRepository.findBlockedUserIdsByBlockerUserId(blocker.getId());
    }

    public List<ModerationReportResponse> findReports(ModerationReportStatus status) {
        List<ModerationReportEntity> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc()
                : reportRepository.findAllByStatusOrderByCreatedAtDesc(status);

        return reports.stream()
                .map(ModerationReportResponse::from)
                .toList();
    }

    @Transactional
    public ModerationReportResponse updateReportStatus(UUID reportId, ModerationReportStatus status) {
        ModerationReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."));

        report.updateStatus(status);
        return ModerationReportResponse.from(report);
    }

    @Transactional
    public void deleteUserModerationData(UUID userId) {
        reportRepository.deleteByReporterUserIdOrTargetUserId(userId, userId);
        blockRepository.deleteByBlockerUserIdOrBlockedUserId(userId, userId);
    }

    private ModerationReportEnvelope createReportEnvelope(ModerationReportEntity report, boolean shouldHideTarget) {
        int reportCount = reportRepository.countByTargetTypeAndTargetIdAndStatusIn(
                report.getTargetType(),
                report.getTargetId(),
                ACTIVE_REPORT_STATUSES
        );

        return new ModerationReportEnvelope(
                ModerationReportResponse.from(report),
                reportCount,
                shouldHideTarget
        );
    }

    private UUID resolveTargetUserId(ModerationReportCreateRequest request) {
        return switch (request.targetType()) {
            case GUESTBOOK_MESSAGE -> guestbookRepository.findById(parseUuid(request.targetId()))
                    .map(GuestbookEntity::getWriterUser)
                    .map(UserEntity::getId)
                    .orElse(request.targetUserId());
            case PUBLIC_BOOK -> bookRepository.findById(parseUuid(request.targetId()))
                    .map(BookEntity::getUserId)
                    .orElse(request.targetUserId());
            case PUBLIC_REVIEW -> reviewRepository.findById(parseUuid(request.targetId()))
                    .map(ReviewEntity::getUserId)
                    .orElse(request.targetUserId());
            case PUBLIC_PROFILE, PUBLIC_BOOKSHELF -> request.targetUserId() != null
                    ? request.targetUserId()
                    : parseUuid(request.targetId());
        };
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고 대상 형식이 올바르지 않습니다.");
        }
    }

    private String serializeSnapshot(Object snapshot) {
        if (snapshot == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고 스냅샷을 저장할 수 없습니다.");
        }
    }
}
