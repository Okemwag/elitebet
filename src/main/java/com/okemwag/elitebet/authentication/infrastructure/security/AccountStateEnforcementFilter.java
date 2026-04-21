package com.okemwag.elitebet.authentication.infrastructure.security;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.okemwag.elitebet.authentication.application.AccountStateAccessService;
import com.okemwag.elitebet.shared.exception.ApiError;
import com.okemwag.elitebet.shared.exception.ErrorCode;
import com.okemwag.elitebet.shared.logging.MdcKeys;
import com.okemwag.elitebet.shared.security.PostAuthenticationFilter;

@Component
public class AccountStateEnforcementFilter extends OncePerRequestFilter implements PostAuthenticationFilter {

	private final AccountStateAccessService accountStateAccessService;

	private final ObjectMapper objectMapper;

	public AccountStateEnforcementFilter(AccountStateAccessService accountStateAccessService, ObjectMapper objectMapper) {
		this.accountStateAccessService = accountStateAccessService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (shouldSkip(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			filterChain.doFilter(request, response);
			return;
		}
		var decision = accountStateAccessService.accessDecision(jwt.getSubject(), request.getRequestURI());
		if (decision.allowed()) {
			filterChain.doFilter(request, response);
			return;
		}
		writeRejection(response, request, decision.rejection().status(), decision.rejection().message());
	}

	private boolean shouldSkip(HttpServletRequest request) {
		String path = request.getRequestURI();
		return request.getMethod().equals("OPTIONS")
				|| path.equals("/api/v1/auth/register")
				|| path.startsWith("/actuator/")
				|| path.equals("/actuator")
				|| path.startsWith("/v3/api-docs")
				|| path.startsWith("/swagger-ui")
				|| path.equals("/swagger-ui.html");
	}

	private void writeRejection(HttpServletResponse response, HttpServletRequest request, int status, String message)
			throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ApiError error = ApiError.of(ErrorCode.FORBIDDEN, message, status, request.getRequestURI(),
				MdcKeys.get(MdcKeys.CORRELATION_ID));
		objectMapper.writeValue(response.getOutputStream(), error);
	}
}
