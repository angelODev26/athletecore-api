package com.athletecore.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de carga del contexto. El seed del admin inicial (V6) exige el hash
 * BCrypt por placeholder: en tests se inyecta el mismo hash de desarrollo que
 * documenta application-local.properties.example (admin / admin123), para que
 * una base limpia quede con el admin documentado y no con una identidad
 * distinta (ver design D4 del change gestion-usuarios-admin).
 */
@SpringBootTest(properties = {
		"spring.flyway.placeholders.admin_username=admin",
		"spring.flyway.placeholders.admin_email=admin@athletecore.local",
		"spring.flyway.placeholders.admin_password_hash=$2y$12$f/H.gisCLENcAAHLhJ/UMeErMGi6WQbiRmggPHT1xTtAwXEWBgHrK"
})
class AthletecoreApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
