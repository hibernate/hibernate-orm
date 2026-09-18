/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.timezones;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupport;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupports;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.descriptor.jdbc.JavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that NORMALIZE timezone storage correctly overrides the JdbcType
 * for OffsetDateTime, ZonedDateTime, and OffsetTime, regardless of the
 * {@code JAVA_TIME_USE_DIRECT_JDBC} setting.
 * <p>
 * Before the fix for HHH-20875, the guard in
 * {@code MetadataBuildingProcess.adaptTimestampTypesToDefaultTimeZoneStorage()}
 * used {@code instanceof JavaTimeJdbcType}, which was always true because the
 * baseline JdbcType registration uses types implementing {@code JavaTimeJdbcType}.
 * This meant the NORMALIZE override was never applied.
 */
@JiraKey("HHH-20875")
public class NormalizeWithDirectJdbcTest {

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.TIMEZONE_DEFAULT_STORAGE, value = "NORMALIZE"),
			@Setting(name = AvailableSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "false")
	})
	@DomainModel(annotatedClasses = Zoned.class)
	@SessionFactory(exportSchema = false)
	void normalizeAppliedWhenDirectJdbcDisabled(DomainModelScope scope) {
		assertNormalizeOverrideApplied( scope );
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = "hibernate.dialect",
					value = "org.hibernate.orm.test.timezones.NormalizeWithDirectJdbcTest$NoDirectJavaTimeDialect"),
			@Setting(name = AvailableSettings.TIMEZONE_DEFAULT_STORAGE, value = "NORMALIZE"),
			@Setting(name = AvailableSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true")
	})
	@DomainModel(annotatedClasses = Zoned.class)
	@SessionFactory(exportSchema = false)
	void normalizeAppliedWhenDialectLacksDirectJdbcSupport(DomainModelScope scope) {
		assertNormalizeOverrideApplied( scope );
	}

	private static void assertNormalizeOverrideApplied(DomainModelScope scope) {
		final PersistentClass binding = scope.getEntityBinding( Zoned.class );

		final JdbcType offsetJdbcType = resolveJdbcType( binding, "offsetDateTime" );
		assertThat( offsetJdbcType )
				.as( "OffsetDateTime should use TimestampJdbcType when NORMALIZE is active" )
				.isInstanceOf( TimestampJdbcType.class );

		final JdbcType zonedJdbcType = resolveJdbcType( binding, "zonedDateTime" );
		assertThat( zonedJdbcType )
				.as( "ZonedDateTime should use TimestampJdbcType when NORMALIZE is active" )
				.isInstanceOf( TimestampJdbcType.class );

		final JdbcType offsetTimeJdbcType = resolveJdbcType( binding, "offsetTime" );
		assertThat( offsetTimeJdbcType )
				.as( "OffsetTime should use TimeJdbcType when NORMALIZE is active" )
				.isInstanceOf( TimeJdbcType.class );
	}

	@Test
	@RequiresDialect( H2Dialect.class )
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.TIMEZONE_DEFAULT_STORAGE, value = "NORMALIZE"),
			@Setting(name = AvailableSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true")
	})
	@DomainModel(annotatedClasses = Zoned.class)
	@SessionFactory(exportSchema = false)
	void normalizeNotAppliedWhenDialectSupportsDirectJdbc(DomainModelScope scope) {
		// H2 uses the default jdbc42() profile: supports OffsetDateTime and
		// OffsetTime but not ZonedDateTime directly. The resolver handles
		// ZonedDateTime via OffsetDateTime fallback, so NORMALIZE should not
		// be applied to any of these types.
		final PersistentClass binding = scope.getEntityBinding( Zoned.class );

		final JdbcType offsetJdbcType = resolveJdbcType( binding, "offsetDateTime" );
		assertThat( offsetJdbcType ).isInstanceOf( JavaTimeJdbcType.class );

		final JdbcType zonedJdbcType = resolveJdbcType( binding, "zonedDateTime" );
		assertThat( zonedJdbcType ).isInstanceOf( JavaTimeJdbcType.class );

		final JdbcType offsetTimeJdbcType = resolveJdbcType( binding, "offsetTime" );
		assertThat( offsetTimeJdbcType ).isInstanceOf( JavaTimeJdbcType.class );
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = "hibernate.dialect",
					value = "org.hibernate.orm.test.timezones.NormalizeWithDirectJdbcTest$ZonedOnlyDialect"),
			@Setting(name = AvailableSettings.TIMEZONE_DEFAULT_STORAGE, value = "NORMALIZE"),
			@Setting(name = AvailableSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true")
	})
	@DomainModel(annotatedClasses = Zoned.class)
	@SessionFactory(exportSchema = false)
	void normalizeAppliedIndependentlyPerType(DomainModelScope scope) {
		// ZonedOnlyDialect supports ZonedDateTime directly but not OffsetDateTime
		// or OffsetTime. Verify that capabilities are tested independently:
		// ZonedDateTime skips NORMALIZE, while OffsetDateTime and OffsetTime get it.
		final PersistentClass binding = scope.getEntityBinding( Zoned.class );

		final JdbcType offsetJdbcType = resolveJdbcType( binding, "offsetDateTime" );
		assertThat( offsetJdbcType )
				.as( "OffsetDateTime should use TimestampJdbcType when dialect lacks OffsetDateTime support" )
				.isInstanceOf( TimestampJdbcType.class );

		final JdbcType zonedJdbcType = resolveJdbcType( binding, "zonedDateTime" );
		assertThat( zonedJdbcType )
				.as( "ZonedDateTime should keep JavaTimeJdbcType when dialect supports it directly" )
				.isInstanceOf( JavaTimeJdbcType.class );

		final JdbcType offsetTimeJdbcType = resolveJdbcType( binding, "offsetTime" );
		assertThat( offsetTimeJdbcType )
				.as( "OffsetTime should use TimeJdbcType when dialect lacks OffsetTime support" )
				.isInstanceOf( TimeJdbcType.class );
	}

	private static JdbcType resolveJdbcType(PersistentClass binding, String propertyName) {
		final BasicValue value = (BasicValue) binding.getProperty( propertyName ).getValue();
		return value.resolve().getJdbcType();
	}

	@Entity(name = "Zoned")
	public static class Zoned {
		@Id
		@GeneratedValue
		Long id;
		OffsetDateTime offsetDateTime;
		ZonedDateTime zonedDateTime;
		OffsetTime offsetTime;
	}

	public static class NoDirectJavaTimeDialect extends H2Dialect {
		@Override
		public DirectJavaTimeJdbcSupport getDirectJavaTimeJdbcSupport() {
			return DirectJavaTimeJdbcSupports.none();
		}
	}

	public static class ZonedOnlyDialect extends H2Dialect {
		@Override
		public DirectJavaTimeJdbcSupport getDirectJavaTimeJdbcSupport() {
			return DirectJavaTimeJdbcSupports.of( ZonedDateTime.class );
		}
	}
}
