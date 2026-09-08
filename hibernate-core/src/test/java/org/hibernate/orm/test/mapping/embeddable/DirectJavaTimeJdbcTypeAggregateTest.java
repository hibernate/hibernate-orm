/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests direct Java Time JDBC types nested in a textual aggregate.
///
/// @author Steve Ebersole
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
@DomainModel(annotatedClasses = DirectJavaTimeJdbcTypeAggregateTest.JsonHolder.class)
@SessionFactory
class DirectJavaTimeJdbcTypeAggregateTest {
	@Test
	void roundTripsEveryDirectJavaTimeType(SessionFactoryScope scope) {
		final LocalDate date = LocalDate.of( 2024, 2, 29 );
		final JavaTimes expected = new JavaTimes();
		expected.localDate = date;
		expected.localTime = LocalTime.of( 13, 14, 15, 123_000_000 );
		expected.localDateTime = LocalDateTime.of( date, expected.localTime );
		expected.offsetTime = expected.localTime.atOffset( ZoneOffset.ofHoursMinutes( 5, 30 ) );
		expected.offsetDateTime = expected.localDateTime.atOffset( ZoneOffset.ofHours( -4 ) );
		expected.zonedDateTime = expected.localDateTime.atZone( ZoneId.of( "Europe/Paris" ) );
		expected.instant = Instant.parse( "2024-02-29T13:14:15.123Z" );
		expected.localDates = new LocalDate[] { date, null, date.plusDays( 1 ) };

		scope.inTransaction( session -> session.persist( new JsonHolder( 1L, expected ) ) );
		scope.inTransaction( session -> {
			final JavaTimes actual = session.find( JsonHolder.class, 1L ).aggregate;
			assertThat( actual.localDate ).isEqualTo( expected.localDate );
			assertThat( actual.localTime ).isEqualTo( expected.localTime );
			assertThat( actual.localDateTime ).isEqualTo( expected.localDateTime );
			assertThat( actual.offsetTime ).isEqualTo( expected.offsetTime );
			assertThat( actual.offsetDateTime ).isEqualTo( expected.offsetDateTime );
			assertThat( actual.zonedDateTime ).isEqualTo( expected.zonedDateTime );
			assertThat( actual.instant ).isEqualTo( expected.instant );
			assertThat( actual.localDates ).containsExactly( expected.localDates );
		} );
	}

	@Entity(name = "DirectJavaTimeJsonHolder")
	static class JsonHolder {
		@Id
		private Long id;

		@JdbcTypeCode(SqlTypes.JSON)
		private JavaTimes aggregate;

		JsonHolder() {
		}

		JsonHolder(Long id, JavaTimes aggregate) {
			this.id = id;
			this.aggregate = aggregate;
		}
	}

	@Embeddable
	static class JavaTimes {
		@JdbcTypeCode(SqlTypes.LOCAL_DATE)
		private LocalDate localDate;

		@JdbcTypeCode(SqlTypes.LOCAL_TIME)
		private LocalTime localTime;

		@JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
		private LocalDateTime localDateTime;

		@JdbcTypeCode(SqlTypes.OFFSET_TIME)
		private OffsetTime offsetTime;

		@JdbcTypeCode(SqlTypes.OFFSET_DATE_TIME)
		private OffsetDateTime offsetDateTime;

		@JdbcTypeCode(SqlTypes.ZONED_DATE_TIME)
		private ZonedDateTime zonedDateTime;

		@JdbcTypeCode(SqlTypes.INSTANT)
		private Instant instant;

		private LocalDate[] localDates;
	}
}
