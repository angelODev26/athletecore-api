package com.athletecore.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de carga del contexto. El seed del admin inicial (V6) exige el hash
 * BCrypt por placeholder: en tests se inyecta un hash fijo para no depender de
 * variables de entorno externas (ver design D4 del change
 * gestion-usuarios-admin).
 */
@SpringBootTest(properties = {
		"spring.flyway.placeholders.admin_username=admin-test",
		"spring.flyway.placeholders.admin_email=admin-test@athletecore.local",
		"spring.flyway.placeholders.admin_password_hash=$2y$12$f/H.gisCLENcAAHLhJ/UMeErMGi6WQbiRmggPHT1xTtAwXEWBgHrK"
})
class AthletecoreApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
