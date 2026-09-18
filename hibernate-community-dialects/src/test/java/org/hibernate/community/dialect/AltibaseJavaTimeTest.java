/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.descriptor.DateTimeUtils;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@RequiresDialect(AltibaseDialect.class)
@Jira("https://hibernate.atlassian.net/browse/HHH-20874")
@DomainModel(annotatedClasses = AltibaseJavaTimeTest.TemporalEntity.class)
public class AltibaseJavaTimeTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SessionFactory
	void testDefaultJavaTimeRoundTrip(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getSessionFactoryOptions()
				.isDirectJavaTimeJdbcAccessEnabled( LocalDateTime.class ) ).isTrue();
		final TemporalEntity original = entityWithNanos( 294520000 );
		scope.inTransaction( session -> session.persist( original ) );
		scope.inTransaction( session -> {
			final TemporalEntity loaded = session.find( TemporalEntity.class, 1 );
			assertThat( loaded.localDate ).isEqualTo( original.localDate );
			assertThat( loaded.localTime ).isEqualTo( original.localTime );
			assertThat( loaded.localDateTime ).isEqualTo( original.localDateTime );
			assertThat( loaded.offsetTime ).isEqualTo( original.offsetTime );
			assertThat( loaded.offsetDateTime.toInstant() ).isEqualTo( original.offsetDateTime.toInstant() );
			assertThat( loaded.zonedDateTime.toInstant() ).isEqualTo( original.zonedDateTime.toInstant() );
		} );
	}

	@ParameterizedTest
	@ValueSource(ints = { 294520789, 999999999 })
	@ServiceRegistry(settings = @Setting(name = AvailableSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "false"))
	@SessionFactory
	void testTimestampTruncationWithoutDirectJdbc(int nanos, SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getSessionFactoryOptions()
				.isDirectJavaTimeJdbcAccessEnabled( LocalDateTime.class ) ).isFalse();
		final TemporalEntity original = entityWithNanos( nanos );
		final var dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final var expected = original.offsetDateTime.toInstant().truncatedTo( ChronoUnit.MICROS );
		scope.inTransaction( session -> session.persist( original ) );
		scope.inTransaction( session -> {
			final TemporalEntity loaded = session.find( TemporalEntity.class, 1 );
			assertThat( loaded.offsetDateTime.toInstant() ).isEqualTo( expected );
			assertThat( loaded.zonedDateTime.toInstant() ).isEqualTo( expected );
			assertThat( DateTimeUtils.adjustToDefaultPrecision( original.offsetDateTime.toInstant(), dialect ) )
					.isEqualTo( loaded.offsetDateTime.toInstant() );
		} );
	}

	private static TemporalEntity entityWithNanos(int nanos) {
		final TemporalEntity entity = new TemporalEntity();
		entity.id = 1;
		entity.localDate = LocalDate.of( 2026, 9, 14 );
		entity.localTime = LocalTime.of( 12, 34, 56 );
		entity.localDateTime = entity.localDate.atTime( entity.localTime ).withNano( nanos );
		entity.offsetTime = entity.localTime.atOffset( ZoneOffset.UTC );
		entity.offsetDateTime = entity.localDateTime.atOffset( ZoneOffset.UTC );
		entity.zonedDateTime = entity.offsetDateTime.toZonedDateTime();
		return entity;
	}

	@Entity(name = "AltibaseTemporalEntity")
	@Table(name = "ALTIBASE_JAVA_TIME")
	public static class TemporalEntity {
		@Id
		Integer id;
		LocalDate localDate;
		LocalTime localTime;
		LocalDateTime localDateTime;
		OffsetTime offsetTime;
		OffsetDateTime offsetDateTime;
		ZonedDateTime zonedDateTime;
	}
}
