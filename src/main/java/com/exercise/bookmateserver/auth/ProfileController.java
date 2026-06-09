package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.user.ProfileResponse;
import com.exercise.bookmateserver.user.ProfileImageStorageService;
import com.exercise.bookmateserver.user.ProfileImageUploadResponse;
import com.exercise.bookmateserver.user.ProfileUpdateRequest;
import com.exercise.bookmateserver.user.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Profile", description = "내 정보 API")
@RestController
public class ProfileController {

    private final AuthService authService;
    private final CurrentUserResolver currentUserResolver;
    private final ProfileImageStorageService profileImageStorageService;

    public ProfileController(
            AuthService authService,
            CurrentUserResolver currentUserResolver,
            ProfileImageStorageService profileImageStorageService
    ) {
        this.authService = authService;
        this.currentUserResolver = currentUserResolver;
        this.profileImageStorageService = profileImageStorageService;
    }

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 프로필과 통계를 조회합니다.")
    @GetMapping("/api/me")
    public ProfileResponse getProfile(HttpServletRequest request) {
        UserEntity user = currentUserResolver.get(request);
        return authService.getProfile(user);
    }

    @Operation(summary = "내 정보 수정", description = "로그인한 사용자의 닉네임과 프로필 이미지를 수정합니다.")
    @PatchMapping("/api/me")
    public ProfileResponse updateProfile(
            HttpServletRequest request,
            @Valid @RequestBody ProfileUpdateRequest profileUpdateRequest
    ) {
        UserEntity user = currentUserResolver.get(request);
        return authService.updateProfile(user, profileUpdateRequest);
    }

    @Operation(summary = "프로필 이미지 업로드", description = "로그인한 사용자의 프로필 이미지를 업로드하고 접근 가능한 이미지 URL을 반환합니다.")
    @PostMapping(value = "/api/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileImageUploadResponse uploadProfileImage(
            HttpServletRequest request,
            @RequestPart("image") MultipartFile image
    ) {
        UserEntity user = currentUserResolver.get(request);
        return profileImageStorageService.store(user.getId(), image);
    }

    @Operation(summary = "회원 탈퇴", description = "로그인한 사용자의 계정과 저장한 책, 단어를 모두 삭제합니다.")
    @DeleteMapping("/api/users/me")
    public void deleteAccount(HttpServletRequest request) {
        UserEntity user = currentUserResolver.get(request);
        authService.deleteAccount(user);
    }
}
