package com.exercise.bookmateserver.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class ProfileImageStorageServiceTest {
    @TempDir Path directory;

    private ProfileImageStorageService service() {
        return new ProfileImageStorageService(directory.toString(), "http://localhost", false,
                "", "auto", "", "", "", "");
    }

    @Test
    void acceptsRealJpegAndPng() throws Exception {
        for (String format : new String[] {"jpeg", "png"}) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, bytes);
            var result = service().store(UUID.randomUUID(),
                    new MockMultipartFile("image", "profile." + format, "image/" + format, bytes.toByteArray()));
            assertThat(result.profileImageUrl()).startsWith("http://localhost/uploads/profile-images/");
        }
    }

    @Test
    void rejectsSvgAndSpoofedJpegWithoutWritingFiles() throws Exception {
        for (String type : new String[] {"image/svg+xml", "image/jpeg"}) {
            assertThatThrownBy(() -> service().store(UUID.randomUUID(), new MockMultipartFile(
                    "image", "profile.jpg", type, "<svg><script>alert(1)</script></svg>".getBytes())))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
        }
        try (var files = java.nio.file.Files.list(directory)) { assertThat(files.count()).isZero(); }
    }

    @Test
    void rejectsOversizeUploads() {
        assertThatThrownBy(() -> service().store(UUID.randomUUID(), new MockMultipartFile(
                "image", "large.jpg", "image/jpeg", new byte[2 * 1024 * 1024 + 1])))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(413));
    }
}
