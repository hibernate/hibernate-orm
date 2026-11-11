package org.hibernate.orm.test.mapping.javatime;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.orm.test.mapping.javatime.redesign.TemporalEligibilityChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
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
@RequiresDialectFeature(feature = TemporalEligibilityChecks.LocalDateTimeMapping.class)
@DomainModel(annotatedClasses = GlobalJavaTimeLocalDateTimeJdbcTypeTests.EntityWithLocalDateTime.class)
@SessionFactory
public class GlobalJavaTimeLocalDateTimeJdbcTypeTests {
	@Test
	void testMappings(DomainModelScope scope, SessionFactoryScope sessionFactoryScope) {
		final var options = sessionFactoryScope.getSessionFactory().getSessionFactoryOptions();
		checkAttribute( scope.getEntityBinding( EntityWithLocalDateTime.class ), "theLocalDateTime",
				options.isDirectJavaTimeJdbcAccessEnabled( LocalDateTime.class ) ? LocalDateTimeJdbcType.class : TimestampJdbcType.class,
				"LocalDateTime" );
	}

	private void checkAttribute(
			PersistentClass entityBinding,
			String attributeName,
			Class<? extends JdbcType> expectedJdbcTypeDescriptorType,
			String expectedTypeName) {
		final Property property = entityBinding.getProperty( attributeName );
		final BasicValue value = (BasicValue) property.getValue();
		final BasicValue.Resolution<?> resolution = value.resolve();
		final JdbcType jdbcType = resolution.getJdbcType();
		assertThat( jdbcType ).isInstanceOf( expectedJdbcTypeDescriptorType );
		assertThat( resolution.getLegacyResolvedBasicType().getName() ).isEqualTo( expectedTypeName );
	}
	@Test
	void testLocalDateTime(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final LocalDateTime start = dialect instanceof SybaseDialect
				? LocalDateTime.now().with( ChronoField.NANO_OF_SECOND, 0L )
				: adjustToDefaultPrecision( LocalDateTime.now(), dialect );

		scope.inTransaction( (session) -> {
			final EntityWithLocalDateTime entity = new EntityWithLocalDateTime();
			entity.id = 1;
			entity.theLocalDateTime = start;
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithLocalDateTime entity = session.get( EntityWithLocalDateTime.class, 1 );
			assertThat( entity.theLocalDateTime ).isEqualTo( start );
			entity.theLocalDateTime = start.plusDays( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithLocalDateTime entity = session.get( EntityWithLocalDateTime.class, 1 );
			assertThat( entity.theLocalDateTime ).isEqualTo( start.plusDays( 2000 ) );
			entity.theLocalDateTime = start.minusDays( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithLocalDateTime entity = session.get( EntityWithLocalDateTime.class, 1 );
			assertThat( entity.theLocalDateTime ).isEqualTo( start.minusDays( 2000 ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "EntityWithLocalDateTime")
	@Table(name = "EntityWithLocalDateTime")
	public static class EntityWithLocalDateTime {
		@Id private Integer id;
		private LocalDateTime theLocalDateTime;
	}
}
