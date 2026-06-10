package com.exercise.bookmateserver.social.guestbook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestbookWriteRequest(
        @NotBlank @Size(max = 1000) String content
) {
}
