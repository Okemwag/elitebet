package com.okemwag.elitebet.integration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class InfrastructureSmokeIT {

	private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18.3-alpine");

	private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:8.6.2-alpine");

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
		.withDatabaseName("elitebet")
		.withUsername("elitebet")
		.withPassword("elitebet_dev_password");

	@Container
	static final GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

	@DynamicPropertySource
	static void registerInfrastructureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", InfrastructureSmokeIT::postgresJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	private static String postgresJdbcUrl() {
		String jdbcUrl = postgres.getJdbcUrl();
		String separator = jdbcUrl.contains("?") ? "&" : "?";
		return jdbcUrl + separator + "currentSchema=elitebet";
	}

	@Test
	void startsWithPostgresRedisAndFlyway(
			@Autowired DataSource dataSource,
			@Autowired RedisConnectionFactory redisConnectionFactory) throws Exception {
		try (var connection = dataSource.getConnection();
				var statement = connection.createStatement();
				var resultSet = statement.executeQuery("""
						select exists(select 1 from information_schema.schemata where schema_name = 'elitebet'),
						       exists(select 1 from information_schema.tables
						              where table_schema = 'elitebet' and table_name = 'flyway_schema_history'),
						       exists(select 1 from information_schema.triggers
						              where event_object_schema = 'elitebet'
						                and event_object_table = 'audit_events'
						                and trigger_name = 'trg_audit_events_append_only_update'),
						       exists(select 1 from information_schema.triggers
						              where event_object_schema = 'elitebet'
						                and event_object_table = 'audit_events'
						                and trigger_name = 'trg_audit_events_append_only_delete')
						""")) {
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getBoolean(1)).isTrue();
			assertThat(resultSet.getBoolean(2)).isTrue();
			assertThat(resultSet.getBoolean(3)).isTrue();
			assertThat(resultSet.getBoolean(4)).isTrue();
		}

		try (var redisConnection = redisConnectionFactory.getConnection()) {
			assertThat(redisConnection.ping()).isEqualTo("PONG");
		}
	}
}
