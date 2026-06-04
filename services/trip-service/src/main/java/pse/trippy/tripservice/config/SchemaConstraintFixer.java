package pse.trippy.tripservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Drops stale CHECK constraints that Hibernate ddl-auto:update cannot modify.
 * This allows new enum values (e.g. PENDING_APPROVAL) to be persisted.
 */
@Slf4j
@Component
public class SchemaConstraintFixer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public SchemaConstraintFixer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        dropCheckConstraintIfExists("trip_schema", "participants", "participants_status_check");
        dropCheckConstraintIfExists("trip_schema", "participants", "participants_role_check");
    }

    private void dropCheckConstraintIfExists(String schema, String table, String constraint) {
        try {
            String sql = String.format(
                    "ALTER TABLE %s.%s DROP CONSTRAINT IF EXISTS %s",
                    schema, table, constraint
            );
            jdbc.execute(sql);
            log.info("Dropped stale constraint {}.{}.{}", schema, table, constraint);
        } catch (Exception e) {
            log.debug("Could not drop constraint {} — may not exist: {}", constraint, e.getMessage());
        }
    }
}
