package com.okemwag.elitebet;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class EliteBetModuleArchitectureTests {

	@Test
	void verifiesModularMonolithBoundaries() {
		ApplicationModules.of(EliteBetApplication.class).verify();
	}
}
