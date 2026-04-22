package com.okemwag.elitebet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.okemwag.elitebet.authentication.domain.repository.AuthAccountRepository;
import com.okemwag.elitebet.user.domain.repository.UserProfileRepository;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
		"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json"
})
class EliteBetApplicationTests {

	@MockitoBean
	private AuthAccountRepository authAccountRepository;

	@MockitoBean
	private UserProfileRepository userProfileRepository;

	@Test
	void contextLoads() {
	}

}
