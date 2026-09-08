/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.javatime;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupport;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupports;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.DateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests Dialect-sensitive direct Java Time JDBC mapping selection.
///
/// @author Steve Ebersole
@MessageKeyInspection(
		messageKey = "HHH006595",
		logger = @Logger(loggerName = CoreMessageLogger.NAME)
)
class DirectJavaTimeJdbcAccessTests {
	@Test
	@ServiceRegistry(settings = {
			@Setting(
					name = JdbcSettings.DIALECT,
					value = "org.hibernate.orm.test.mapping.javatime.DirectJavaTimeJdbcAccessTests$NoDirectJavaTimeDialect"
			),
			@Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true")
	})
	@DomainModel(annotatedClasses = EntityWithLocalDates.class)
	@SessionFactory(exportSchema = false)
	@SuppressWarnings("deprecation")
	void explicitPreferenceUsesDialectFallbackAndWarnsOnce(
			DomainModelScope domainModelScope,
			SessionFactoryScope sessionFactoryScope,
			MessageKeyWatcher warningWatcher) {
		final var options = sessionFactoryScope.getSessionFactory().getSessionFactoryOptions();
		assertThat( options.isPreferJavaTimeJdbcTypesEnabled() ).isTrue();
		assertThat( options.isDirectJavaTimeJdbcAccessEnabled( LocalDate.class ) ).isFalse();

		final PersistentClass entityBinding = domainModelScope.getEntityBinding( EntityWithLocalDates.class );
		final BasicValue inferred = (BasicValue) entityBinding.getProperty( "inferred" ).getValue();
		final BasicValue explicit = (BasicValue) entityBinding.getProperty( "explicit" ).getValue();

		assertThat( inferred.resolve().getJdbcType() ).isInstanceOf( DateJdbcType.class );
		assertThat( inferred.resolve().getLegacyResolvedBasicType().getName() ).isEqualTo( "LocalDate" );
		assertThat( explicit.resolve().getJdbcType() ).isInstanceOf( LocalDateJdbcType.class );

		assertThat( warningWatcher.getTriggeredMessages() )
				.singleElement()
				.asString()
				.contains(
						"not be JDBC 4.2 compliant",
						"Java Time type(s) [LocalDate]",
						"error-prone conversions to [java.sql.Date]",
						"filing a bug report"
				);
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(
					name = JdbcSettings.DIALECT,
					value = "org.hibernate.orm.test.mapping.javatime.DirectJavaTimeJdbcAccessTests$NoDirectJavaTimeDialect"
			),
			@Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "false")
	})
	@DomainModel(annotatedClasses = EntityWithLocalDates.class)
	@SessionFactory(exportSchema = false)
	void disabledPreferenceFallsBackSilently(MessageKeyWatcher warningWatcher) {
		assertThat( warningWatcher.wasTriggered() ).isFalse();
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(
					name = JdbcSettings.DIALECT,
					value = "org.hibernate.orm.test.mapping.javatime.DirectJavaTimeJdbcAccessTests$NoDirectJavaTimeDialect"
			),
			@Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true")
	})
	@DomainModel(annotatedClasses = EntityWithOffsets.class)
	@SessionFactory(exportSchema = false)
	void offsetTypesShareOneWarning(MessageKeyWatcher warningWatcher) {
		assertThat( warningWatcher.getTriggeredMessages() )
				.singleElement()
				.asString()
				.contains(
						"Java Time type(s) [OffsetTime/OffsetDateTime]",
						"error-prone conversions to [java.sql.Time/java.sql.Timestamp]"
				);
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(
					name = JdbcSettings.DIALECT,
					value = "org.hibernate.orm.test.mapping.javatime.DirectJavaTimeJdbcAccessTests$NoDirectJavaTimeDialect"
			),
			@Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true")
	})
	@DomainModel(annotatedClasses = EntityWithExplicitLocalDate.class)
	@SessionFactory(exportSchema = false)
	void explicitJdbcTypeDoesNotWarn(MessageKeyWatcher warningWatcher) {
		assertThat( warningWatcher.wasTriggered() ).isFalse();
	}

	@Entity(name = "EntityWithLocalDates")
	static class EntityWithLocalDates {
		@Id
		private Integer id;

		private LocalDate inferred;
		private LocalDate anotherInferred;

		@JdbcTypeCode(SqlTypes.LOCAL_DATE)
		private LocalDate explicit;
	}

	@Entity(name = "EntityWithOffsets")
	static class EntityWithOffsets {
		@Id
		private Integer id;

		private OffsetTime offsetTime;
		private OffsetDateTime offsetDateTime;
	}

	@Entity(name = "EntityWithExplicitLocalDate")
	static class EntityWithExplicitLocalDate {
		@Id
		private Integer id;

		@JdbcTypeCode(SqlTypes.LOCAL_DATE)
		private LocalDate explicit;
	}

	public static class NoDirectJavaTimeDialect extends H2Dialect {
		@Override
		public DirectJavaTimeJdbcSupport getDirectJavaTimeJdbcSupport() {
			return DirectJavaTimeJdbcSupports.none();
		}
	}
}
