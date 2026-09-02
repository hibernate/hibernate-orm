/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.literal;

import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Consumer;

import org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.type.descriptor.DateTimeUtils;

import org.junit.jupiter.api.Test;

import static org.hibernate.dialect.literal.spi.ZeroOffsetLiteralStyle.NUMERIC_OFFSET;
import static org.hibernate.dialect.literal.spi.ZeroOffsetLiteralStyle.UTC_DESIGNATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies the supported datetime-literal rendering facility against the
/// existing internal formatting behavior.
///
/// @author Steve Ebersole
public class StandardDateTimeLiteralRenderingTest {
	private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );
	private static final TimeZone NEW_YORK = TimeZone.getTimeZone( "America/New_York" );

	@Test
	void matchesDateAndLocalTimeRendering() {
		final LocalDate localDate = LocalDate.of( 2026, 8, 30 );
		final LocalTime localTime = LocalTime.of( 12, 34, 56 );
		final Date date = Date.from( Instant.parse( "2026-08-30T12:34:56Z" ) );
		final java.sql.Date sqlDate = java.sql.Date.valueOf( localDate );
		final java.sql.Time sqlTime = java.sql.Time.valueOf( localTime );
		final Calendar calendar = Calendar.getInstance( NEW_YORK );
		calendar.setTimeInMillis( Instant.parse( "2026-08-30T12:34:56Z" ).toEpochMilli() );

		assertParity(
				appender -> DateTimeUtils.appendAsDate( appender, localDate ),
				appender -> StandardDateTimeLiteralRendering.appendAsDate( appender, localDate )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsDate( appender, date ),
				appender -> StandardDateTimeLiteralRendering.appendAsDate( appender, date )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsDate( appender, sqlDate ),
				appender -> StandardDateTimeLiteralRendering.appendAsDate( appender, sqlDate )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsDate( appender, calendar ),
				appender -> StandardDateTimeLiteralRendering.appendAsDate( appender, calendar )
		);

		assertParity(
				appender -> DateTimeUtils.appendAsLocalTime( appender, localTime ),
				appender -> StandardDateTimeLiteralRendering.appendAsLocalTime( appender, localTime )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsLocalTime( appender, date ),
				appender -> StandardDateTimeLiteralRendering.appendAsLocalTime( appender, date )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsLocalTime( appender, sqlTime ),
				appender -> StandardDateTimeLiteralRendering.appendAsLocalTime( appender, sqlTime )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsLocalTime( appender, calendar ),
				appender -> StandardDateTimeLiteralRendering.appendAsLocalTime( appender, calendar )
		);
	}

	@Test
	void matchesStandardTimeRendering() {
		final OffsetTime offsetTime = OffsetTime.of( 12, 34, 56, 0, ZoneOffset.ofHoursMinutes( 5, 30 ) );
		final LocalTime localTime = offsetTime.toLocalTime();
		final Date date = Date.from( Instant.parse( "2026-08-30T12:34:56Z" ) );
		final Calendar calendar = Calendar.getInstance( NEW_YORK );
		calendar.setTime( date );

		for ( boolean supportsOffset : new boolean[] { false, true } ) {
			assertParity(
					appender -> DateTimeUtils.appendAsTime( appender, offsetTime, supportsOffset, NEW_YORK ),
					appender -> StandardDateTimeLiteralRendering.appendAsTime(
							appender,
							offsetTime,
							supportsOffset,
							NEW_YORK
					)
			);
			assertParity(
					appender -> DateTimeUtils.appendAsTime( appender, localTime, supportsOffset, NEW_YORK ),
					appender -> StandardDateTimeLiteralRendering.appendAsTime(
							appender,
							localTime,
							supportsOffset,
							NEW_YORK
					)
			);
		}

		assertParity(
				appender -> DateTimeUtils.appendAsTime( appender, date, NEW_YORK ),
				appender -> StandardDateTimeLiteralRendering.appendAsTime( appender, date, NEW_YORK )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsTime( appender, calendar, NEW_YORK ),
				appender -> StandardDateTimeLiteralRendering.appendAsTime( appender, calendar, NEW_YORK )
		);
	}

	@Test
	void matchesStandardTimestampRendering() {
		final OffsetDateTime offsetDateTime = OffsetDateTime.of(
				2026,
				8,
				30,
				12,
				34,
				56,
				123_456_789,
				ZoneOffset.ofHoursMinutes( -3, -30 )
		);
		final LocalDateTime localDateTime = offsetDateTime.toLocalDateTime();
		final ZonedDateTime zonedDateTime = offsetDateTime.atZoneSameInstant( ZoneId.of( "Europe/Paris" ) );
		final Instant instant = offsetDateTime.toInstant();

		for ( boolean supportsOffset : new boolean[] { false, true } ) {
			assertTimestampParity( offsetDateTime, supportsOffset, NEW_YORK );
			assertTimestampParity( localDateTime, supportsOffset, NEW_YORK );
			assertTimestampParity( zonedDateTime, supportsOffset, NEW_YORK );
			assertTimestampParity( instant, supportsOffset, NEW_YORK );
		}
	}

	@Test
	void matchesLegacyTimestampRendering() {
		final Timestamp timestamp = Timestamp.from( Instant.parse( "2026-08-30T12:34:56.123456789Z" ) );
		final Date date = Date.from( timestamp.toInstant() );
		final Calendar calendar = Calendar.getInstance( NEW_YORK );
		calendar.setTime( date );

		assertParity(
				appender -> DateTimeUtils.appendAsTimestampWithMillis( appender, timestamp, UTC ),
				appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMillis( appender, timestamp, UTC )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsTimestampWithMicros( appender, timestamp, UTC ),
				appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMicros( appender, timestamp, UTC )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsTimestampWithNanos( appender, timestamp, UTC ),
				appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithNanos( appender, timestamp, UTC )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsTimestampWithMicros( appender, date, UTC ),
				appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMicros( appender, date, UTC )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsTimestampWithNanos( appender, date, UTC ),
				appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithNanos( appender, date, UTC )
		);
		assertParity(
				appender -> DateTimeUtils.appendAsTimestampWithMillis( appender, calendar, UTC ),
				appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMillis( appender, calendar, UTC )
		);
	}

	@Test
	void rendersExplicitZeroOffsetStyles() {
		final OffsetTime offsetTime = OffsetTime.of( 12, 34, 56, 0, ZoneOffset.UTC );
		final OffsetDateTime offsetDateTime = OffsetDateTime.of(
				2026,
				8,
				30,
				12,
				34,
				56,
				123_456_789,
				ZoneOffset.UTC
		);

		assertEquals(
				"12:34:56Z",
				render( appender -> StandardDateTimeLiteralRendering.appendAsTime(
						appender,
						offsetTime,
						true,
						UTC,
						UTC_DESIGNATOR
				) )
		);
		assertEquals(
				"12:34:56+00:00",
				render( appender -> StandardDateTimeLiteralRendering.appendAsTime(
						appender,
						offsetTime,
						true,
						UTC,
						NUMERIC_OFFSET
				) )
		);
		assertEquals(
				"2026-08-30 12:34:56.123Z",
				render( appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMillis(
						appender,
						offsetDateTime,
						true,
						UTC,
						UTC_DESIGNATOR
				) )
		);
		assertEquals(
				"2026-08-30 12:34:56.123456+00:00",
				render( appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMicros(
						appender,
						offsetDateTime,
						true,
						UTC,
						NUMERIC_OFFSET
				) )
		);
		assertEquals(
				"2026-08-30 12:34:56.123456789+00:00",
				render( appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithNanos(
						appender,
						offsetDateTime,
						true,
						UTC,
						NUMERIC_OFFSET
				) )
		);
		assertEquals(
				"2026-08-30 12:34:56.123456789",
				render( appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithNanos(
						appender,
						offsetDateTime.toLocalDateTime(),
						true,
						UTC,
						NUMERIC_OFFSET
				) )
		);
	}

	@Test
	void zeroOffsetStyleDoesNotChangeOtherPaths() {
		final OffsetDateTime nonzero = OffsetDateTime.of(
				2026,
				8,
				30,
				12,
				34,
				56,
				123_456_000,
				ZoneOffset.ofHours( 2 )
		);
		assertEquals(
				render( appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMicros(
						appender,
						nonzero,
						true,
						UTC,
						UTC_DESIGNATOR
				) ),
				render( appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMicros(
						appender,
						nonzero,
						true,
						UTC,
						NUMERIC_OFFSET
				) )
		);
		assertEquals(
				render( appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMicros(
						appender,
						nonzero,
						false,
						NEW_YORK,
						UTC_DESIGNATOR
				) ),
				render( appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMicros(
						appender,
						nonzero,
						false,
						NEW_YORK,
						NUMERIC_OFFSET
				) )
		);
	}

	@Test
	void rejectsMissingArgumentsAndUnsupportedShapes() {
		assertThrows(
				NullPointerException.class,
				() -> StandardDateTimeLiteralRendering.appendAsDate( null, LocalDate.now() )
		);
		assertThrows(
				NullPointerException.class,
				() -> StandardDateTimeLiteralRendering.appendAsDate( appender(), (LocalDate) null )
		);
		assertThrows(
				NullPointerException.class,
				() -> StandardDateTimeLiteralRendering.appendAsTime(
						appender(),
						LocalTime.NOON,
						true,
						null,
						UTC_DESIGNATOR
				)
		);
		assertThrows(
				NullPointerException.class,
				() -> StandardDateTimeLiteralRendering.appendAsTimestampWithMillis(
						appender(),
						LocalDateTime.now(),
						true,
						UTC,
						null
				)
		);
		assertThrows(
				DateTimeException.class,
				() -> StandardDateTimeLiteralRendering.appendAsTime( appender(), LocalDate.now(), false, UTC )
		);
	}

	private static void assertTimestampParity(
			java.time.temporal.TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		assertParity(
				appender -> DateTimeUtils.appendAsTimestampWithMillis(
						appender,
						temporalAccessor,
						supportsOffset,
						jdbcTimeZone
				),
				appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMillis(
						appender,
						temporalAccessor,
						supportsOffset,
						jdbcTimeZone
				)
		);
		assertParity(
				appender -> DateTimeUtils.appendAsTimestampWithMicros(
						appender,
						temporalAccessor,
						supportsOffset,
						jdbcTimeZone
				),
				appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithMicros(
						appender,
						temporalAccessor,
						supportsOffset,
						jdbcTimeZone
				)
		);
		assertParity(
				appender -> DateTimeUtils.appendAsTimestampWithNanos(
						appender,
						temporalAccessor,
						supportsOffset,
						jdbcTimeZone
				),
				appender -> StandardDateTimeLiteralRendering.appendAsTimestampWithNanos(
						appender,
						temporalAccessor,
						supportsOffset,
						jdbcTimeZone
				)
		);
	}

	private static void assertParity(Consumer<SqlAppender> internal, Consumer<SqlAppender> supported) {
		assertEquals( render( internal ), render( supported ) );
	}

	private static String render(Consumer<SqlAppender> rendering) {
		final var result = new StringBuilder();
		rendering.accept( new StringBuilderSqlAppender( result ) );
		return result.toString();
	}

	private static SqlAppender appender() {
		return new StringBuilderSqlAppender( new StringBuilder() );
	}
}
