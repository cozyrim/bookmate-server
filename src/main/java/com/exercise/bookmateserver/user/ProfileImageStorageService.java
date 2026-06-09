package com.exercise.bookmateserver.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProfileImageStorageService {

    private final Path uploadDirectory;
    private final String publicBaseUrl;

    public ProfileImageStorageService(
            @Value("${app.upload.profile-image-dir:uploads/profile-images}") String uploadDirectory,
            @Value("${app.public-base-url:http://127.0.0.1:8080}") String publicBaseUrl
    ) {
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = removeTrailingSlash(publicBaseUrl);
    }

    public ProfileImageUploadResponse store(UUID userId, MultipartFile image) {
        validateImage(image);

        try {
            Files.createDirectories(uploadDirectory);

            String extension = resolveExtension(image);
            String fileName = userId + "-" + UUID.randomUUID() + extension;
            Path targetPath = uploadDirectory.resolve(fileName).normalize();

            if (!targetPath.startsWith(uploadDirectory)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 파일 이름입니다.");
            }

            image.transferTo(targetPath.toFile());

            String imageUrl = publicBaseUrl + "/uploads/profile-images/" + fileName;
            return new ProfileImageUploadResponse(imageUrl);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 이미지를 저장하지 못했습니다.", exception);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 이미지가 필요합니다.");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private String resolveExtension(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null) {
            return ".jpg";
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}
