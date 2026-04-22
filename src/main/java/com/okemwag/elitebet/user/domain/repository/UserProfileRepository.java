package com.okemwag.elitebet.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.okemwag.elitebet.user.domain.model.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
	Optional<UserProfile> findByPrincipalId(String principalId);
}
