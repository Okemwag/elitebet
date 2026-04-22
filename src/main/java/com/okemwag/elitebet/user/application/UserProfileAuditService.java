package com.okemwag.elitebet.user.application;

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

@Service
public class UserProfileAuditService {

	private final ObjectProvider<AuditEventPublisher> auditEventPublisher;

	public UserProfileAuditService(ObjectProvider<AuditEventPublisher> auditEventPublisher) {
		this.auditEventPublisher = auditEventPublisher;
	}

	public void profileCreated(String principalId, HttpServletRequest request) {
		publish(AuditAction.CREATE, principalId, request);
	}

	public void profileUpdated(String principalId, HttpServletRequest request) {
		publish(AuditAction.UPDATE, principalId, request);
	}

	private void publish(AuditAction action, String principalId, HttpServletRequest request) {
		AuditEventPublisher publisher = auditEventPublisher.getIfAvailable();
		if (publisher == null) {
			return;
		}
		publisher.publish(new AuditContext(principalId, AuditActorType.USER,
				request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER), request.getRemoteAddr(),
				request.getHeader("User-Agent")), action, AuditCategory.SECURITY,
				new AuditMetadata("USER_PROFILE", principalId, null, null, Map.of()));
	}
}
