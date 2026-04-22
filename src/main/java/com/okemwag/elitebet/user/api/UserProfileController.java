package com.okemwag.elitebet.user.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.okemwag.elitebet.shared.logging.MdcKeys;
import com.okemwag.elitebet.shared.response.ApiResponse;
import com.okemwag.elitebet.user.api.request.UpsertUserProfileRequest;
import com.okemwag.elitebet.user.application.UserProfileAuditService;
import com.okemwag.elitebet.user.application.UserProfileService;
import com.okemwag.elitebet.user.application.dto.UserProfileView;

@Validated
@RestController
@RequestMapping("/api/v1/user/profile")
public class UserProfileController {

	private final UserProfileService userProfileService;

	private final UserProfileAuditService auditService;

	public UserProfileController(UserProfileService userProfileService, UserProfileAuditService auditService) {
		this.userProfileService = userProfileService;
		this.auditService = auditService;
	}

	@GetMapping
	public ApiResponse<UserProfileView> get(Authentication authentication) {
		return ApiResponse.ok(userProfileService.get(principalId(authentication)), MdcKeys.get(MdcKeys.CORRELATION_ID));
	}

	@PutMapping
	public ResponseEntity<ApiResponse<UserProfileView>> upsert(Authentication authentication,
			@Valid @RequestBody UpsertUserProfileRequest request,
			HttpServletRequest httpRequest) {
		String principalId = principalId(authentication);
		UserProfileService.UpsertResult result = userProfileService.upsert(principalId, request);
		if (result.created()) {
			auditService.profileCreated(principalId, httpRequest);
		}
		else {
			auditService.profileUpdated(principalId, httpRequest);
		}
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
			.body(ApiResponse.ok(result.profile(), MdcKeys.get(MdcKeys.CORRELATION_ID)));
	}

	private String principalId(Authentication authentication) {
		return ((Jwt) authentication.getPrincipal()).getSubject();
	}
}
