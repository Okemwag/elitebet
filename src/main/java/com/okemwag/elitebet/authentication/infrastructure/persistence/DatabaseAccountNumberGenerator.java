package com.okemwag.elitebet.authentication.infrastructure.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.okemwag.elitebet.authentication.application.AccountNumberGenerator;

@Component
@ConditionalOnBean(JdbcClient.class)
public class DatabaseAccountNumberGenerator implements AccountNumberGenerator {

	private final JdbcClient jdbcClient;

	public DatabaseAccountNumberGenerator(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public String nextAccountNumber() {
		return jdbcClient.sql("select 'EB' || nextval('elitebet.account_number_seq')::text")
			.query(String.class)
			.single();
	}
}
