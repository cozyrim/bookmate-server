package com.exercise.bookmateserver.auth;

public record EmailAvailabilityResponse(
        String email,
        boolean available,
        String message
) {
}
