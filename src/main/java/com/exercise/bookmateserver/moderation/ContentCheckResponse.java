package com.exercise.bookmateserver.moderation;

import java.util.List;

public record ContentCheckResponse(
        boolean allowed,
        List<String> reasons
) {
}
