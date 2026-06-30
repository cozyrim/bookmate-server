package com.exercise.bookmateserver.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProfileImageStorageService {

    private static final long MAX_PROFILE_IMAGE_SIZE_BYTES = 2L * 1024L * 1024L;

    private final Path uploadDirectory;
    private final String publicBaseUrl;
    private final boolean r2Enabled;
    private final String r2Bucket;
    private final String r2PublicBaseUrl;
    private final S3Client s3Client;

    public ProfileImageStorageService(
            @Value("${app.upload.profile-image-dir:uploads/profile-images}") String uploadDirectory,
            @Value("${app.public-base-url:http://127.0.0.1:8080}") String publicBaseUrl,
            @Value("${app.r2.enabled:false}") boolean r2Enabled,
            @Value("${app.r2.endpoint:}") String r2Endpoint,
            @Value("${app.r2.region:auto}") String r2Region,
            @Value("${app.r2.bucket:}") String r2Bucket,
            @Value("${app.r2.access-key-id:}") String r2AccessKeyId,
            @Value("${app.r2.secret-access-key:}") String r2SecretAccessKey,
            @Value("${app.r2.public-base-url:}") String r2PublicBaseUrl
    ) {
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = removeTrailingSlash(publicBaseUrl);
        this.r2Enabled = r2Enabled;
        this.r2Bucket = r2Bucket.trim();
        this.r2PublicBaseUrl = removeTrailingSlash(r2PublicBaseUrl.trim());
        this.s3Client = r2Enabled
                ? createR2Client(r2Endpoint, r2Region, this.r2Bucket, r2AccessKeyId, r2SecretAccessKey, this.r2PublicBaseUrl)
                : null;
    }

    public ProfileImageUploadResponse store(UUID userId, MultipartFile image) {
        validateImage(image);

        if (r2Enabled) {
            return storeInR2(userId, image);
        }

        return storeLocally(userId, image);
    }

    private ProfileImageUploadResponse storeInR2(UUID userId, MultipartFile image) {
        String extension = resolveExtension(image);
        String objectKey = "profile-images/" + userId + "/" + UUID.randomUUID() + extension;
        String contentType = resolveContentType(image);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(r2Bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(image.getBytes()));

            return new ProfileImageUploadResponse(r2PublicBaseUrl + "/" + objectKey);
        } catch (IOException | SdkException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 이미지를 저장하지 못했습니다.", exception);
        }
    }

    private ProfileImageUploadResponse storeLocally(UUID userId, MultipartFile image) {
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

    private S3Client createR2Client(
            String endpoint,
            String region,
            String bucket,
            String accessKeyId,
            String secretAccessKey,
            String publicBaseUrl
    ) {
        String normalizedEndpoint = endpoint.trim();
        String normalizedRegion = region.trim().isEmpty() ? "auto" : region.trim();
        String normalizedAccessKeyId = accessKeyId.trim();
        String normalizedSecretAccessKey = secretAccessKey.trim();

        if (normalizedEndpoint.isEmpty()
                || bucket.isEmpty()
                || normalizedAccessKeyId.isEmpty()
                || normalizedSecretAccessKey.isEmpty()
                || publicBaseUrl.isEmpty()) {
            throw new IllegalStateException("R2 storage is enabled, but one or more R2 environment variables are missing.");
        }

        try {
            return S3Client.builder()
                    .endpointOverride(URI.create(normalizedEndpoint))
                    .region(Region.of(normalizedRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(normalizedAccessKeyId, normalizedSecretAccessKey)
                    ))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .build();
        } catch (IllegalArgumentException | SdkException exception) {
            throw new IllegalStateException("R2 storage configuration is invalid.", exception);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 이미지가 필요합니다.");
        }

        if (image.getSize() > MAX_PROFILE_IMAGE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "프로필 이미지는 2MB 이하로 업로드해주세요.");
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

    private String resolveContentType(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return "image/jpeg";
        }

        return contentType.toLowerCase(Locale.ROOT);
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}
