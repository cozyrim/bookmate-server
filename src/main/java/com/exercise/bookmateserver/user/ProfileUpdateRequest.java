package com.exercise.bookmateserver.user;

import com.exercise.bookmateserver.auth.NicknamePolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(max = NicknamePolicy.MAX_LENGTH, message = "닉네임은 8자 이하로 입력해주세요.")
        String nickname,
        String profileImageUrl,
        Boolean isPublic,
        String roomTheme
) {
}
