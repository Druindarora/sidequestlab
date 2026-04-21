package dev.sidequestlab.backend.health;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileControllerTest {

    private static final String PROFILE_SQL = "SELECT full_name FROM profile WHERE id_profile = 1";

    @Test
    void meReturnsDtoWithDatabaseFullNameWhenDatabaseValueIsPresent() {
        StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate();
        jdbcTemplate.returnValue = "Ada Lovelace";
        ProfileController controller = new ProfileController(jdbcTemplate);

        ProfileDto result = controller.me();

        assertThat(result.fullName()).isEqualTo("Ada Lovelace");
        assertThat(result.title()).isEqualTo("Développeur Java / Angular");
        assertThat(result.summary()).isEqualTo("Résumé rapide de qui je suis.");
        assertThat(jdbcTemplate.lastSql).isEqualTo(PROFILE_SQL);
        assertThat(jdbcTemplate.callCount).isEqualTo(1);
    }

    @Test
    void meReturnsFallbackFullNameWhenDatabaseValueIsNull() {
        StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate();
        jdbcTemplate.returnValue = null;
        ProfileController controller = new ProfileController(jdbcTemplate);

        ProfileDto result = controller.me();

        assertThat(result.fullName()).isEqualTo("Unknown from backend");
        assertThat(result.title()).isEqualTo("Développeur Java / Angular");
        assertThat(result.summary()).isEqualTo("Résumé rapide de qui je suis.");
        assertThat(jdbcTemplate.lastSql).isEqualTo(PROFILE_SQL);
        assertThat(jdbcTemplate.callCount).isEqualTo(1);
    }

    @Test
    void mePropagatesJdbcFailureBecauseControllerDoesNotHandleIt() {
        StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate();
        jdbcTemplate.exceptionToThrow = new DataAccessResourceFailureException("DB unavailable");
        ProfileController controller = new ProfileController(jdbcTemplate);

        assertThatThrownBy(controller::me).isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(jdbcTemplate.lastSql).isEqualTo(PROFILE_SQL);
        assertThat(jdbcTemplate.callCount).isEqualTo(1);
    }

    private static final class StubJdbcTemplate extends JdbcTemplate {
        private String returnValue;
        private RuntimeException exceptionToThrow;
        private String lastSql;
        private int callCount;

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType) {
            this.lastSql = sql;
            this.callCount++;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return requiredType.cast(returnValue);
        }
    }
}
