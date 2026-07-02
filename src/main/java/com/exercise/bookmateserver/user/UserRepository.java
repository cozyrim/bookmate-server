package com.exercise.bookmateserver.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByNicknameIgnoreCase(String nickname);

    boolean existsByNicknameIgnoreCaseAndIdNot(String nickname, UUID id);

    Optional<UserEntity> findByProviderAndProviderIdAndDeletedAtIsNull(AuthProvider provider, String providerId);

    long countByDeletedAtIsNull();

    long countByProviderAndDeletedAtIsNull(AuthProvider provider);

    List<UserEntity> findByNicknameContainingIgnoreCaseAndIsPublicTrueAndDeletedAtIsNull(String nickname);

    List<UserEntity> findByIsPublicTrueAndDeletedAtIsNull();

    Optional<UserEntity> findByIdAndIsPublicTrueAndDeletedAtIsNull(UUID id);

    @Query(value = "SELECT * FROM app_users WHERE is_public = true AND deleted_at IS NULL AND id != :excludeId ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<UserEntity> findRandomPublicUserExcluding(@Param("excludeId") UUID excludeId);
}
