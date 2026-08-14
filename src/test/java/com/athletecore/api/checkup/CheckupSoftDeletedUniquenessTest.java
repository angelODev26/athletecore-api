package com.athletecore.api.checkup;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.athletecore.api.athlete.Athlete;
import com.athletecore.api.athlete.AthleteRepository;

/**
 * Tests de integración JPA (Fase D-3) del scenario de la spec
 * checkup-registration: "Soft-deleted checkup does not block a new one".
 *
 * La unicidad de chequeos (athlete_id, year, month, category) y de tiempos
 * (checkup_id, style, distance) se implementa con índices únicos parciales
 * (WHERE deleted_at IS NULL), por lo que un chequeo/tiempo soft-deleted no
 * bloquea la creación de uno nuevo con el mismo tuplet.
 *
 * Usa la PostgreSQL real del entorno (V4 aplicada) con ddl-auto=validate;
 * @DataJpaTest corre cada test en una transacción que se revierte al final
 * (rollback), por lo que no se persiste data entre runs.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/athletecore",
        "spring.datasource.username=athletecore_user",
        "spring.datasource.password=athletecore_pass",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
class CheckupSoftDeletedUniquenessTest {

    @Autowired
    private CheckupRepository checkupRepository;

    @Autowired
    private CheckupTimeRepository checkupTimeRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Athlete createAthlete(String suffix) {
        Athlete athlete = Athlete.builder()
                .username("test_softdel_" + suffix)
                .email("test_softdel_" + suffix + "@example.com")
                .firstName("Test")
                .lastName("SoftDelete")
                .build();
        return athleteRepository.save(athlete);
    }

    private Checkup createCheckup(Athlete athlete, Integer year, Integer month, String category) {
        Checkup checkup = Checkup.builder()
                .athlete(athlete)
                .year(year)
                .month(month)
                .category(category)
                .build();
        return checkupRepository.save(checkup);
    }

    private CheckupTime createCheckupTime(Checkup checkup, String style, Integer distance) {
        CheckupTime checkupTime = CheckupTime.builder()
                .checkup(checkup)
                .style(style)
                .distance(distance)
                .timeSeconds(new BigDecimal("65.250"))
                .build();
        return checkupTimeRepository.save(checkupTime);
    }

    @Test
    @DisplayName("crear nuevo chequeo no colisiona con uno soft-deleted del mismo tuplet (athlete, year, month, category)")
    void crearNuevoChequeo_noColisiona_conChequeoSoftDeleted_delMismoTuplet() {
        Athlete athlete = createAthlete("unique_checkup_" + System.nanoTime());
        Integer year = 2026;
        Integer month = 8;
        String category = "MAYOR";

        // 1. Primer chequeo activo
        Checkup first = createCheckup(athlete, year, month, category);
        assertThat(checkupRepository.existsActiveByAthleteIdAndYearMonthAndCategory(
                athlete.getId(), year, month, category)).isTrue();

        // 2. Soft delete del primero (mismo mecanismo que CheckupService)
        first.setDeletedAt(Instant.now());
        checkupRepository.save(first);
        entityManager.flush();

        // 3. El tuplet ya no está ocupado por filas activas
        assertThat(checkupRepository.existsActiveByAthleteIdAndYearMonthAndCategory(
                athlete.getId(), year, month, category)).isFalse();

        // 4. Nuevo chequeo con el mismo tuplet: no debe lanzar (índice único
        //    parcial WHERE deleted_at IS NULL no incluye la fila borrada)
        Checkup second = createCheckup(athlete, year, month, category);
        entityManager.flush();
        assertThat(second.getId()).isNotNull();
        assertThat(checkupRepository.existsActiveByAthleteIdAndYearMonthAndCategory(
                athlete.getId(), year, month, category)).isTrue();

        // 5. Ambas filas existen en la tabla: una soft-deleted y una activa
        Number totalRows = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM checkups WHERE athlete_id = :athleteId")
                .setParameter("athleteId", athlete.getId())
                .getSingleResult();
        assertThat(totalRows.longValue()).isEqualTo(2L);

        Number softDeletedRows = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM checkups WHERE id = :id AND deleted_at IS NOT NULL")
                .setParameter("id", first.getId())
                .getSingleResult();
        assertThat(softDeletedRows.longValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findActiveByAthleteId excluye chequeos soft-deleted del listado activo")
    void findActiveByAthleteId_excluyeChequeosSoftDeleted() {
        Athlete athlete = createAthlete("listado_" + System.nanoTime());

        Checkup active = createCheckup(athlete, 2026, 8, "MAYOR");
        Checkup deleted = createCheckup(athlete, 2026, 7, "MAYOR");
        deleted.setDeletedAt(Instant.now());
        checkupRepository.save(deleted);
        entityManager.flush();

        List<Checkup> result = checkupRepository.findActiveByAthleteId(athlete.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(active.getId());
        assertThat(result.get(0).getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("existsActiveByAthleteIdAndYearMonthAndCategory devuelve false si todos los chequeos del tuplet están soft-deleted")
    void existsActive_devuelveFalse_cuandoTodosLosChequeosDelTupletEstanBorrados() {
        Athlete athlete = createAthlete("edge_" + System.nanoTime());

        Checkup checkup = createCheckup(athlete, 2026, 8, "MAYOR");
        checkup.setDeletedAt(Instant.now());
        checkupRepository.save(checkup);
        entityManager.flush();

        assertThat(checkupRepository.existsActiveByAthleteIdAndYearMonthAndCategory(
                athlete.getId(), 2026, 8, "MAYOR")).isFalse();
        assertThat(checkupRepository.findActiveByAthleteId(athlete.getId())).isEmpty();
    }

    @Test
    @DisplayName("tiempo de prueba soft-deleted no bloquea uno nuevo para el mismo (checkup, style, distance)")
    void agregarCheckupTime_noColisiona_conUnoSoftDeleted_delMismoStyleYDistance() {
        Athlete athlete = createAthlete("tiempo_" + System.nanoTime());
        Checkup checkup = createCheckup(athlete, 2026, 8, "MAYOR");

        // 1. Primer tiempo activo (LIBRE, 100)
        CheckupTime first = createCheckupTime(checkup, "LIBRE", 100);
        assertThat(checkupTimeRepository.existsActiveByCheckupIdAndStyleAndDistance(
                checkup.getId(), "LIBRE", 100)).isTrue();

        // 2. Soft delete del primero
        first.setDeletedAt(Instant.now());
        checkupTimeRepository.save(first);
        entityManager.flush();

        // 3. El par (style, distance) ya no está ocupado por filas activas
        assertThat(checkupTimeRepository.existsActiveByCheckupIdAndStyleAndDistance(
                checkup.getId(), "LIBRE", 100)).isFalse();

        // 4. Nuevo tiempo con el mismo par: no debe lanzar (índice único
        //    parcial WHERE deleted_at IS NULL no incluye la fila borrada)
        CheckupTime second = createCheckupTime(checkup, "LIBRE", 100);
        entityManager.flush();
        assertThat(second.getId()).isNotNull();
        assertThat(checkupTimeRepository.existsActiveByCheckupIdAndStyleAndDistance(
                checkup.getId(), "LIBRE", 100)).isTrue();

        // 5. Ambas filas existen en la tabla: una soft-deleted y una activa
        Number totalRows = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM checkup_times WHERE checkup_id = :checkupId")
                .setParameter("checkupId", checkup.getId())
                .getSingleResult();
        assertThat(totalRows.longValue()).isEqualTo(2L);
    }
}
