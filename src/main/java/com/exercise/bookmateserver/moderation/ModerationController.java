package com.exercise.bookmateserver.moderation;

import com.exercise.bookmateserver.auth.CurrentUserResolver;
import com.exercise.bookmateserver.user.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Moderation", description = "신고, 차단, 콘텐츠 점검 API")
@RestController
public class ModerationController {

    private final ModerationService moderationService;
    private final CurrentUserResolver currentUserResolver;

    public ModerationController(ModerationService moderationService, CurrentUserResolver currentUserResolver) {
        this.moderationService = moderationService;
        this.currentUserResolver = currentUserResolver;
    }

    @Operation(summary = "콘텐츠 신고", description = "공개 콘텐츠를 신고하고 신고자 화면에서 즉시 숨김 처리할 수 있는 응답을 반환합니다.")
    @PostMapping("/api/moderation/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ModerationReportEnvelope submitReport(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ModerationReportCreateRequest request
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return moderationService.submitReport(user, request);
    }

    @Operation(summary = "사용자 차단", description = "공개 유저를 차단합니다.")
    @PostMapping("/api/moderation/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public UserBlockResponse blockUser(
            HttpServletRequest httpRequest,
            @Valid @RequestBody BlockUserRequest request
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return moderationService.blockUser(user, request);
    }

    @Operation(summary = "차단 해제", description = "차단한 사용자를 해제합니다.")
    @DeleteMapping("/api/moderation/blocks/{blockedUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblockUser(HttpServletRequest httpRequest, @PathVariable UUID blockedUserId) {
        UserEntity user = currentUserResolver.get(httpRequest);
        moderationService.unblockUser(user, blockedUserId);
    }

    @Operation(summary = "차단 목록 조회", description = "내가 차단한 사용자 목록을 조회합니다.")
    @GetMapping("/api/moderation/blocks")
    public List<UserBlockResponse> findBlockedUsers(HttpServletRequest httpRequest) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return moderationService.findBlockedUsers(user);
    }

    @Operation(summary = "콘텐츠 작성 전 점검", description = "방명록, 프로필, 리뷰 등 공개될 수 있는 텍스트를 점검합니다.")
    @PostMapping("/api/moderation/content/check")
    public ContentCheckResponse checkContent(@Valid @RequestBody ContentCheckRequest request) {
        return moderationService.checkContent(request);
    }
}
