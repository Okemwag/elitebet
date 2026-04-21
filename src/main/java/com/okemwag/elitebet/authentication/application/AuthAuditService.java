package com.okemwag.elitebet.authentication.application;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.okemwag.elitebet.shared.auditing.AuditAction;
import com.okemwag.elitebet.shared.auditing.AuditActorType;
import com.okemwag.elitebet.shared.auditing.AuditCategory;
import com.okemwag.elitebet.shared.auditing.AuditContext;
import com.okemwag.elitebet.shared.auditing.AuditEventPublisher;
import com.okemwag.elitebet.shared.auditing.AuditMetadata;
import com.okemwag.elitebet.shared.logging.CorrelationIdFilter;
import com.okemwag.elitebet.shared.security.SecurityUtils;

@Service
public class AuthAuditService {

	private final ObjectProvider<AuditEventPublisher> auditEventPublisher;

	public AuthAuditService(ObjectProvider<AuditEventPublisher> auditEventPublisher) {
		this.auditEventPublisher = auditEventPublisher;
	}

	public void registrationCreated(String principalId, String email, HttpServletRequest request) {
		publish(AuditAction.CREATE, principalId, actorContext("anonymous", AuditActorType.USER, request),
				Map.of("email", email));
	}

	public void accountLocked(String principalId, String reason, HttpServletRequest request) {
		publish(AuditAction.LOCK, principalId, actorContext(SecurityUtils.currentPrincipalIdOrAnonymous(),
				AuditActorType.ADMIN, request), Map.of("reason", reason));
	}

	public void accountUnlocked(String principalId, String reason, HttpServletRequest request) {
		publish(AuditAction.UNLOCK, principalId, actorContext(SecurityUtils.currentPrincipalIdOrAnonymous(),
				AuditActorType.ADMIN, request), Map.of("reason", reason));
	}

	public void accountDisabled(String principalId, String reason, HttpServletRequest request) {
		publish(AuditAction.LOCK, principalId, actorContext(SecurityUtils.currentPrincipalIdOrAnonymous(),
				AuditActorType.ADMIN, request), Map.of("reason", reason, "operation", "disable"));
	}

	public void logout(String principalId, HttpServletRequest request) {
		publish(AuditAction.LOGOUT, principalId, actorContext(principalId, AuditActorType.USER, request), Map.of());
	}

	private void publish(AuditAction action, String principalId, AuditContext context, Map<String, String> values) {
		AuditEventPublisher publisher = auditEventPublisher.getIfAvailable();
		if (publisher == null) {
			return;
		}
		publisher.publish(context, action, AuditCategory.SECURITY,
				new AuditMetadata("AUTH_ACCOUNT", principalId, null, null, values));
	}

	private AuditContext actorContext(String actorId, AuditActorType actorType, HttpServletRequest request) {
		return new AuditContext(actorId, actorType, request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER),
				request.getRemoteAddr(), request.getHeader("User-Agent"));
	}
}
