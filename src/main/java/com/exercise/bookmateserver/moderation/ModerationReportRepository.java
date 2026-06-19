package com.exercise.bookmateserver.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModerationReportRepository extends JpaRepository<ModerationReportEntity, UUID> {

    Optional<ModerationReportEntity> findFirstByReporterUserIdAndTargetTypeAndTargetIdAndStatusInOrderByCreatedAtDesc(
            UUID reporterUserId,
            ModerationTargetType targetType,
            String targetId,
            Collection<ModerationReportStatus> statuses
    );

    boolean existsByReporterUserIdAndTargetTypeAndTargetIdAndStatusIn(
            UUID reporterUserId,
            ModerationTargetType targetType,
            String targetId,
            Collection<ModerationReportStatus> statuses
    );

    int countByTargetTypeAndTargetIdAndStatusIn(
            ModerationTargetType targetType,
            String targetId,
            Collection<ModerationReportStatus> statuses
    );

    List<ModerationReportEntity> findAllByStatusOrderByCreatedAtDesc(ModerationReportStatus status);

    List<ModerationReportEntity> findAllByOrderByCreatedAtDesc();

    void deleteByReporterUserIdOrTargetUserId(UUID reporterUserId, UUID targetUserId);
}
