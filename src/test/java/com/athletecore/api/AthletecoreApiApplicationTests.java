package com.athletecore.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de carga del contexto. El seed del admin inicial (V6) exige el hash
 * BCrypt por placeholder: en tests se usa una identidad propia
 * (context-admin) con un hash de un valor no documentado, para no sembrar
 * credenciales conocidas en la BD configurada ni colisionar con un `admin`
 * real que tenga otro hash (ver design D4 del change gestion-usuarios-admin).
 * Aun así, debe ejecutarse contra una base de test dedicada.
 */
@SpringBootTest(properties = {
		"spring.flyway.placeholders.admin_username=context-admin",
		"spring.flyway.placeholders.admin_email=context-admin@athletecore.local",
		"spring.flyway.placeholders.admin_password_hash=$2b$12$mDiRwpsrSYB76kLlz3.2CeH/oYtVfxRIcMji1h/bZqIL/7qEqioMK"
})
class AthletecoreApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
