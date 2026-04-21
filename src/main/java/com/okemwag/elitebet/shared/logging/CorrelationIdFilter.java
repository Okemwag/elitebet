package com.okemwag.elitebet.shared.logging;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String correlationId = resolveCorrelationId(request);
		MDC.put(MdcKeys.CORRELATION_ID, correlationId);
		response.setHeader(CORRELATION_ID_HEADER, correlationId);
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			MDC.remove(MdcKeys.CORRELATION_ID);
			MDC.remove(MdcKeys.USER_ID);
		}
	}

	private String resolveCorrelationId(HttpServletRequest request) {
		String correlationId = request.getHeader(CORRELATION_ID_HEADER);
		if (StringUtils.hasText(correlationId) && correlationId.length() <= 128) {
			return correlationId;
		}
		return UUID.randomUUID().toString();
	}
}
