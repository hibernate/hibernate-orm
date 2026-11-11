package org.hibernate.orm.test.mapping.javatime;

import java.time.LocalDate;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.orm.test.mapping.javatime.redesign.TemporalEligibilityChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.DateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;

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
@RequiresDialectFeature(feature = TemporalEligibilityChecks.LocalDateMapping.class)
@DomainModel(annotatedClasses = GlobalJavaTimeLocalDateJdbcTypeTests.EntityWithLocalDate.class)
@SessionFactory
public class GlobalJavaTimeLocalDateJdbcTypeTests {
	@Test
	void testMappings(DomainModelScope scope, SessionFactoryScope sessionFactoryScope) {
		final var options = sessionFactoryScope.getSessionFactory().getSessionFactoryOptions();
		checkAttribute( scope.getEntityBinding( EntityWithLocalDate.class ), "theLocalDate",
				options.isDirectJavaTimeJdbcAccessEnabled( LocalDate.class ) ? LocalDateJdbcType.class : DateJdbcType.class,
				"LocalDate" );
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
	void testLocalDate(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final LocalDate startTime = adjustToDefaultPrecision( LocalDate.now(), dialect );

		scope.inTransaction( (session) -> {
			final EntityWithLocalDate entity = new EntityWithLocalDate();
			entity.id = 1;
			entity.theLocalDate = startTime;
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithLocalDate entity = session.get( EntityWithLocalDate.class, 1 );
			assertThat( entity.theLocalDate ).isEqualTo( startTime );
			entity.theLocalDate = startTime.plusDays( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithLocalDate entity = session.get( EntityWithLocalDate.class, 1 );
			assertThat( entity.theLocalDate ).isEqualTo( startTime.plusDays( 2000 ) );
			entity.theLocalDate = startTime.minusDays( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithLocalDate entity = session.get( EntityWithLocalDate.class, 1 );
			assertThat( entity.theLocalDate ).isEqualTo( startTime.minusDays( 2000 ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "EntityWithLocalDate")
	@Table(name = "EntityWithLocalDate")
	public static class EntityWithLocalDate {
		@Id private Integer id;
		private LocalDate theLocalDate;
	}
}
