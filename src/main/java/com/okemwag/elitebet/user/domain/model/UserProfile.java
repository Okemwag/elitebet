package com.okemwag.elitebet.user.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import com.okemwag.elitebet.user.domain.enums.UserProfileStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "user_profiles", schema = "elitebet",
		uniqueConstraints = @UniqueConstraint(name = "uq_user_profiles_principal_id", columnNames = "principal_id"))
public class UserProfile {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "principal_id", nullable = false, length = 160)
	private String principalId;

	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 80)
	private String lastName;

	@Column(name = "date_of_birth", nullable = false)
	private LocalDate dateOfBirth;

	@Column(name = "country_code", nullable = false, length = 2)
	private String countryCode;

	@Column(name = "region_code", length = 80)
	private String regionCode;

	@Column(name = "phone_number", nullable = false, length = 32)
	private String phoneNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private UserProfileStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected UserProfile() {
	}

	private UserProfile(String principalId, String firstName, String lastName, LocalDate dateOfBirth,
			String countryCode, String regionCode, String phoneNumber, Instant now) {
		this.principalId = principalId;
		this.createdAt = now;
		apply(firstName, lastName, dateOfBirth, countryCode, regionCode, phoneNumber, now);
	}

	public static UserProfile create(String principalId, String firstName, String lastName, LocalDate dateOfBirth,
			String countryCode, String regionCode, String phoneNumber, Instant now) {
		return new UserProfile(principalId, firstName, lastName, dateOfBirth, countryCode, regionCode, phoneNumber, now);
	}

	public void update(String firstName, String lastName, LocalDate dateOfBirth, String countryCode, String regionCode,
			String phoneNumber, Instant now) {
		apply(firstName, lastName, dateOfBirth, countryCode, regionCode, phoneNumber, now);
	}

	private void apply(String firstName, String lastName, LocalDate dateOfBirth, String countryCode, String regionCode,
			String phoneNumber, Instant now) {
		this.firstName = firstName.trim();
		this.lastName = lastName.trim();
		this.dateOfBirth = dateOfBirth;
		this.countryCode = countryCode.trim().toUpperCase(Locale.ROOT);
		this.regionCode = regionCode == null || regionCode.isBlank() ? null : regionCode.trim().toUpperCase(Locale.ROOT);
		this.phoneNumber = phoneNumber.trim();
		this.status = UserProfileStatus.COMPLETE;
		this.updatedAt = now;
	}

	public UUID id() {
		return id;
	}

	public String principalId() {
		return principalId;
	}

	public String firstName() {
		return firstName;
	}

	public String lastName() {
		return lastName;
	}

	public LocalDate dateOfBirth() {
		return dateOfBirth;
	}

	public String countryCode() {
		return countryCode;
	}

	public String regionCode() {
		return regionCode;
	}

	public String phoneNumber() {
		return phoneNumber;
	}

	public UserProfileStatus status() {
		return status;
	}
}
