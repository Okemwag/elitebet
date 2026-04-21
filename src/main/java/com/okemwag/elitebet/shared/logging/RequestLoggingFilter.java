package com.okemwag.elitebet.shared.logging;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.okemwag.elitebet.shared.security.SecurityUtils;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Instant startedAt = Instant.now();
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			MDC.put(MdcKeys.USER_ID, SecurityUtils.currentPrincipalIdOrAnonymous());
			long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
			log.info("http_request method={} path={} status={} durationMs={} remoteAddr={}", request.getMethod(),
					request.getRequestURI(), response.getStatus(), durationMs, request.getRemoteAddr());
			MDC.remove(MdcKeys.USER_ID);
		}
	}
}
