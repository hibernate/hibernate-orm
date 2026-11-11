package org.hibernate.orm.test.mapping.javatime;

import java.time.LocalTime;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.orm.test.mapping.javatime.redesign.TemporalEligibilityChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeJdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.type.descriptor.DateTimeUtils.adjustToPrecision;

/// Direct-setting descriptor and lifecycle coverage, isolated by domain type.
/// An unsupported sibling type must not prevent this type's assertions running.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true"))
@RequiresDialectFeature(feature = TemporalEligibilityChecks.LocalTimeMapping.class)
@DomainModel(annotatedClasses = GlobalJavaTimeLocalTimeJdbcTypeTests.EntityWithLocalTime.class)
@SessionFactory
public class GlobalJavaTimeLocalTimeJdbcTypeTests {
	@Test
	void testMappings(DomainModelScope scope, SessionFactoryScope sessionFactoryScope) {
		final var options = sessionFactoryScope.getSessionFactory().getSessionFactoryOptions();
		checkAttribute( scope.getEntityBinding( EntityWithLocalTime.class ), "theLocalTime",
				options.isDirectJavaTimeJdbcAccessEnabled( LocalTime.class ) ? LocalTimeJdbcType.class : TimeJdbcType.class,
				"LocalTime" );
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
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle drivers truncate fractional seconds from the LocalTime")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA time type does not support fractional seconds")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase drivers truncate fractional seconds from the LocalTime")
	void testLocalTime(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final LocalTime startTime = adjustToPrecision( LocalTime.now(), 0, dialect );

		scope.inTransaction( (session) -> {
			final EntityWithLocalTime entity = new EntityWithLocalTime();
			entity.id = 1;
			entity.theLocalTime = startTime;
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithLocalTime entity = session.get( EntityWithLocalTime.class, 1 );
			assertThat( entity.theLocalTime ).isEqualTo( startTime );
			entity.theLocalTime = startTime.plusHours( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithLocalTime entity = session.get( EntityWithLocalTime.class, 1 );
			assertThat( entity.theLocalTime ).isEqualTo( startTime.plusHours( 2000 ) );
			entity.theLocalTime = startTime.plusHours( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithLocalTime entity = session.get( EntityWithLocalTime.class, 1 );
			assertThat( entity.theLocalTime ).isEqualTo( startTime.plusHours( 2000 ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "EntityWithLocalTime")
	@Table(name = "EntityWithLocalTime")
	public static class EntityWithLocalTime {
		@Id private Integer id;
		private LocalTime theLocalTime;
	}
}
