package com.exercise.bookmateserver.moderation;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
public class ModerationAdminController {

    private final ModerationService moderationService;
    private final String adminSecret;

    public ModerationAdminController(
            ModerationService moderationService,
            @Value("${app.moderation.admin-secret:}") String adminSecret
    ) {
        this.moderationService = moderationService;
        this.adminSecret = adminSecret;
    }

    @GetMapping("/api/admin/moderation/reports")
    public List<ModerationReportResponse> findReports(
            @RequestHeader(name = "X-Admin-Secret", required = false) String requestedSecret,
            @RequestParam(required = false) ModerationReportStatus status
    ) {
        validateAdmin(requestedSecret);
        return moderationService.findReports(status);
    }

    @PatchMapping("/api/admin/moderation/reports/{reportId}/status")
    public ModerationReportResponse updateReportStatus(
            @RequestHeader(name = "X-Admin-Secret", required = false) String requestedSecret,
            @PathVariable UUID reportId,
            @Valid @RequestBody ModerationReportStatusUpdateRequest request
    ) {
        validateAdmin(requestedSecret);
        return moderationService.updateReportStatus(reportId, request.status());
    }

    private void validateAdmin(String requestedSecret) {
        if (adminSecret == null || adminSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (!adminSecret.equals(requestedSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 권한이 필요합니다.");
        }
    }
}
