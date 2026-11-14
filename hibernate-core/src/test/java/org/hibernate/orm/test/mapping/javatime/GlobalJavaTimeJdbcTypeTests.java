package org.hibernate.orm.test.mapping.javatime;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.orm.junit.Setting;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.type.descriptor.DateTimeUtils.adjustToDefaultPrecision;

/// Direct-setting descriptor and lifecycle coverage, isolated by domain type.
/// An unsupported sibling type must not prevent this type's assertions running.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true"))
@DomainModel(annotatedClasses = GlobalJavaTimeJdbcTypeTests.EntityWithInstant.class)
@SessionFactory
public class GlobalJavaTimeJdbcTypeTests {
	@Test
	void testInstant(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Instant start = adjustToDefaultPrecision( Instant.EPOCH, dialect );

		scope.inTransaction( (session) -> {
			final EntityWithInstant entity = new EntityWithInstant();
			entity.id = 1;
			entity.theInstant = start;
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithInstant entity = session.get( EntityWithInstant.class, 1 );
			assertThat( entity.theInstant ).isEqualTo( start );
			entity.theInstant = start.plus( 2000, ChronoUnit.DAYS );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithInstant entity = session.get( EntityWithInstant.class, 1 );
			assertThat( entity.theInstant ).isEqualTo( start.plus( 2000, ChronoUnit.DAYS ) );
			entity.theInstant = start.minus( 2000, ChronoUnit.DAYS );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithInstant entity = session.get( EntityWithInstant.class, 1 );
			assertThat( entity.theInstant ).isEqualTo( start.minus( 2000, ChronoUnit.DAYS ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "EntityWithInstant")
	@Table(name = "EntityWithInstant")
	public static class EntityWithInstant {
		@Id private Integer id;
		private Instant theInstant;
	}
}
