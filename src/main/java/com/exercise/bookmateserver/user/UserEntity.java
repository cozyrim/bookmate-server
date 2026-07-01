package com.exercise.bookmateserver.user;

import com.exercise.bookmateserver.auth.NicknamePolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "app_users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_app_users_nickname", columnNames = "nickname"),
                @UniqueConstraint(name = "uk_app_users_provider_id", columnNames = {"provider", "provider_id"})
        }
)
public class UserEntity {

    private static final String DEFAULT_NICKNAME = "북메이트";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(nullable = false, length = NicknamePolicy.MAX_LENGTH)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_public", nullable = false, columnDefinition = "boolean default true")
    private boolean isPublic = true;

    @Column(name = "room_theme", nullable = false, columnDefinition = "varchar(255) default 'AppBackground'")
    private String roomTheme = "AppBackground";

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected UserEntity() {
    }

    private UserEntity(
            String email,
            String passwordHash,
            AuthProvider provider,
            String providerId,
            String nickname,
            String profileImageUrl
    ) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = normalizeNickname(nickname);
        this.profileImageUrl = normalizeNullable(profileImageUrl);
    }

    public static UserEntity createLocal(String email, String passwordHash, String nickname, String profileImageUrl) {
        return new UserEntity(email, passwordHash, AuthProvider.LOCAL, null, nickname, profileImageUrl);
    }

    public static UserEntity createKakao(String providerId, String email, String nickname, String profileImageUrl) {
        return new UserEntity(email, null, AuthProvider.KAKAO, providerId, nickname, profileImageUrl);
    }

    public void updateProfile(String nickname, String profileImageUrl, boolean isPublic, String roomTheme) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = normalizeNickname(nickname);
        }
        this.profileImageUrl = normalizeNullable(profileImageUrl);
        this.isPublic = isPublic;
        if (roomTheme != null && !roomTheme.isBlank()) {
            this.roomTheme = roomTheme;
        }
    }

    public void updateKakaoProfile(String email, String nickname, String profileImageUrl) {
        if (this.email == null || this.email.isBlank()) {
            this.email = normalizeEmail(email);
        }

        if (this.nickname == null || this.nickname.isBlank() || DEFAULT_NICKNAME.equals(this.nickname)) {
            this.nickname = normalizeNickname(nickname);
        }

        if (this.profileImageUrl == null || this.profileImageUrl.isBlank()) {
            this.profileImageUrl = normalizeNullable(profileImageUrl);
        }
    }

    public void updateKakaoAccountInfo(String email) {
        if (this.email == null || this.email.isBlank()) {
            this.email = normalizeEmail(email);
        }
    }

    public void anonymizeForAccountDeletion() {
        String suffix = id == null
                ? UUID.randomUUID().toString().replace("-", "").substring(0, 6)
                : id.toString().replace("-", "").substring(0, 6);

        this.email = null;
        this.passwordHash = null;
        this.providerId = null;
        this.nickname = "탈퇴" + suffix;
        this.profileImageUrl = null;
        this.isPublic = false;
        this.roomTheme = "AppBackground";
        this.deletedAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase();
    }

    private String normalizeNickname(String nickname) {
        String normalized = NicknamePolicy.normalize(nickname);
        if (normalized == null) {
            return DEFAULT_NICKNAME;
        }

        NicknamePolicy.validate(normalized);
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getRoomTheme() {
        return roomTheme;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
