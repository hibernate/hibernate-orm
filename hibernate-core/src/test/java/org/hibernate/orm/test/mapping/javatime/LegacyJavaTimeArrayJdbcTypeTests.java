package org.hibernate.orm.test.mapping.javatime;

import java.time.OffsetDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/// Retains the legacy PostgreSQL array test's instant-only expectation.
/// This is compatibility coverage, not enabled direct-setting coverage.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@DomainModel(annotatedClasses = LegacyJavaTimeArrayJdbcTypeTests.EntityWithJavaTimeValues.class)
@SessionFactory
class LegacyJavaTimeArrayJdbcTypeTests {

	@Test
	@RequiresDialect(value = PostgreSQLDialect.class)
	@SkipForDialect( dialectClass = SpannerPostgreSQLDialect.class,
	reason = "Spanner PostgreSQL does not support WITH expressions for INSERT statements")
	void testArray(SessionFactoryScope scope) {
		final var offsetDateTime = OffsetDateTime.parse("1977-07-24T12:34:56+02:00");
		scope.inTransaction( session -> {
			final var nativeQuery = session.createNativeQuery(
			"WITH data AS (SELECT unnest(?) AS id, unnest(?) AS offset_date_time)"
			+ " INSERT INTO EntityWithJavaTimeValues (id, theOffsetDateTime) SELECT * FROM data"
			);
			nativeQuery.setParameter( 1, new int[] { 1 } );
			nativeQuery.setParameter( 2, new OffsetDateTime[] { offsetDateTime } );
			assertThat( nativeQuery.executeUpdate() ).isEqualTo( 1 );
			final var found = session.find( EntityWithJavaTimeValues.class, 1 );
			assertThat( found.theOffsetDateTime.toInstant() ).isEqualTo( offsetDateTime.toInstant() );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
	@Entity(name = "EntityWithJavaTimeValues")
	@Table(name = "EntityWithJavaTimeValues")
	static class EntityWithJavaTimeValues {
		@Id Integer id;
		OffsetDateTime theOffsetDateTime;
	}
}
