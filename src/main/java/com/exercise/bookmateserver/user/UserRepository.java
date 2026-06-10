package com.exercise.bookmateserver.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UserEntity> findByProviderAndProviderId(AuthProvider provider, String providerId);

    java.util.List<UserEntity> findByNicknameContainingIgnoreCaseAndIsPublicTrue(String nickname);

    Optional<UserEntity> findByIdAndIsPublicTrue(UUID id);

    @Query(value = "SELECT * FROM users WHERE is_public = true AND id != :excludeId ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<UserEntity> findRandomPublicUserExcluding(@Param("excludeId") UUID excludeId);
}
