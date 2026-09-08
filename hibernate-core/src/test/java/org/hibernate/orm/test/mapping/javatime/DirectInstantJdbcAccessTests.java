/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.javatime;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.SqlTypes;

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

/// Tests Dialect-sensitive handling of an explicitly preferred direct `Instant`
/// JDBC type.
///
/// @author Steve Ebersole
@MessageKeyInspection(
		messageKey = "HHH006596",
		logger = @Logger(loggerName = CoreMessageLogger.NAME)
)
class DirectInstantJdbcAccessTests {
	@Test
	@ServiceRegistry(settings = {
			@Setting(
					name = JdbcSettings.DIALECT,
					value = "org.hibernate.orm.test.mapping.javatime.DirectJavaTimeJdbcAccessTests$NoDirectJavaTimeDialect"
			),
			@Setting(name = MappingSettings.PREFERRED_INSTANT_JDBC_TYPE, value = "INSTANT")
	})
	@DomainModel(annotatedClasses = DirectInstantJdbcAccessTests.EntityWithInstants.class)
	@SessionFactory(exportSchema = false)
	void unsupportedPreferenceWarnsAndFallsBack(
			DomainModelScope domainModelScope,
			SessionFactoryScope sessionFactoryScope,
			MessageKeyWatcher warningWatcher) {
		assertThat( sessionFactoryScope.getSessionFactory().getSessionFactoryOptions().getPreferredSqlTypeCodeForInstant() )
				.isEqualTo( SqlTypes.TIMESTAMP_UTC );

		final var entityBinding = domainModelScope.getEntityBinding( EntityWithInstants.class );
		final BasicValue inferred = (BasicValue) entityBinding.getProperty( "inferred" ).getValue();
		final BasicValue explicit = (BasicValue) entityBinding.getProperty( "explicit" ).getValue();

		assertThat( inferred.resolve().getJdbcType().getJdbcTypeCode() ).isNotEqualTo( SqlTypes.INSTANT );
		assertThat( explicit.resolve().getJdbcType().getJdbcTypeCode() ).isEqualTo( SqlTypes.INSTANT );

		assertThat( warningWatcher.getTriggeredMessages() )
				.singleElement()
				.asString()
				.contains(
						"Java Time type(s) [Instant]",
						"fall back to [TIMESTAMP_UTC] handling",
						"filing a feature request"
				);
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(
					name = JdbcSettings.DIALECT,
					value = "org.hibernate.orm.test.mapping.javatime.DirectJavaTimeJdbcAccessTests$NoDirectJavaTimeDialect"
			),
			@Setting(name = MappingSettings.PREFERRED_INSTANT_JDBC_TYPE, value = "INSTANT")
	})
	@DomainModel(annotatedClasses = EntityWithExplicitInstant.class)
	@SessionFactory(exportSchema = false)
	void explicitJdbcTypeDoesNotWarn(MessageKeyWatcher warningWatcher) {
		assertThat( warningWatcher.wasTriggered() ).isFalse();
	}

	@Entity(name = "EntityWithInstants")
	static class EntityWithInstants {
		@Id
		private Integer id;

		private Instant inferred;
		private Instant anotherInferred;

		@JdbcTypeCode(SqlTypes.INSTANT)
		private Instant explicit;
	}

	@Entity(name = "EntityWithExplicitInstant")
	static class EntityWithExplicitInstant {
		@Id
		private Integer id;

		@JdbcTypeCode(SqlTypes.INSTANT)
		private Instant explicit;
	}
}
