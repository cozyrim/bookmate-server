package com.exercise.bookmateserver.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNicknameIgnoreCase(String nickname);

    boolean existsByNicknameIgnoreCaseAndIdNot(String nickname, UUID id);

    Optional<UserEntity> findByProviderAndProviderId(AuthProvider provider, String providerId);

    List<UserEntity> findByNicknameContainingIgnoreCaseAndIsPublicTrue(String nickname);

    List<UserEntity> findByIsPublicTrue();

    Optional<UserEntity> findByIdAndIsPublicTrue(UUID id);

    @Query(value = "SELECT * FROM app_users WHERE is_public = true AND id != :excludeId ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<UserEntity> findRandomPublicUserExcluding(@Param("excludeId") UUID excludeId);
}
