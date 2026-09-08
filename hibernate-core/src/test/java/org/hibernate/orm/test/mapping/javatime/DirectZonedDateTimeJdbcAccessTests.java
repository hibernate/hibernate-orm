/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.javatime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupport;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupports;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests warnings for Dialect-sensitive direct `ZonedDateTime` JDBC fallbacks.
///
/// @author Steve Ebersole
@MessageKeyInspection(
		messageKey = "HHH006596",
		logger = @Logger(loggerName = CoreMessageLogger.NAME)
)
class DirectZonedDateTimeJdbcAccessTests {
	@Test
	@ServiceRegistry(settings = {
			@Setting(
					name = JdbcSettings.DIALECT,
					value = "org.hibernate.orm.test.mapping.javatime.DirectZonedDateTimeJdbcAccessTests$NoDirectJavaTimeDialect"
			),
			@Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true")
	})
	@DomainModel(annotatedClasses = EntityWithZonedDateTimes.class)
	@SessionFactory(exportSchema = false)
	void standardFallbackWarnsOnce(MessageKeyWatcher warningWatcher) {
		assertThat( warningWatcher.getTriggeredMessages() )
				.singleElement()
				.asString()
				.contains(
						"Java Time type(s) [ZonedDateTime]",
						"error-prone conversions to [java.sql.Timestamp]",
						"filing a feature request"
				);
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(
					name = JdbcSettings.DIALECT,
					value = "org.hibernate.orm.test.mapping.javatime.DirectZonedDateTimeJdbcAccessTests$OffsetDirectJavaTimeDialect"
			),
			@Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true")
	})
	@DomainModel(annotatedClasses = EntityWithZonedDateTimes.class)
	@SessionFactory(exportSchema = false)
	void offsetConversionWarnsOnce(MessageKeyWatcher warningWatcher) {
		assertThat( warningWatcher.getTriggeredMessages() )
				.singleElement()
				.asString()
				.contains(
						"Java Time type(s) [ZonedDateTime]",
						"convert values to [java.time.OffsetDateTime] for direct JDBC access",
						"filing a feature request"
				);
	}

	@Entity(name = "EntityWithZonedDateTimes")
	static class EntityWithZonedDateTimes {
		@Id
		private Integer id;

		private ZonedDateTime first;
		private ZonedDateTime second;
	}

	public static class NoDirectJavaTimeDialect extends H2Dialect {
		@Override
		public DirectJavaTimeJdbcSupport getDirectJavaTimeJdbcSupport() {
			return DirectJavaTimeJdbcSupports.none();
		}
	}

	public static class OffsetDirectJavaTimeDialect extends H2Dialect {
		@Override
		public DirectJavaTimeJdbcSupport getDirectJavaTimeJdbcSupport() {
			return DirectJavaTimeJdbcSupports.of(
					LocalDate.class,
					LocalTime.class,
					LocalDateTime.class,
					OffsetTime.class,
					OffsetDateTime.class
			);
		}
	}
}
