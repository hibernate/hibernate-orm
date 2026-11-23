package org.hibernate.orm.test.type.descriptor.java;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/// Regression tests for unwrapping temporal values to the requested Date subclass.
///
/// @author Steve Ebersole
@BaseUnitTest
@JiraKey("HHH-18532")
class DateSubclassesUnwrapTest {
	@ParameterizedTest
	@ValueSource(strings = { "1969-07-01T12:34:56.789Z", "2025-07-01T12:34:56.789Z" })
	void testJavaTimeConstruction(String text) {
		final var instant = Instant.parse( text );
		final var localDateTime = LocalDateTime.ofInstant( instant, ZoneId.systemDefault() );
		assertDateSubclasses( LocalDateTimeJavaType.INSTANCE, localDateTime, instant.toEpochMilli() );
		assertDateSubclasses(
				OffsetDateTimeJavaType.INSTANCE,
				instant.atOffset( ZoneOffset.ofHoursMinutes( 5, 30 ) ),
				instant.toEpochMilli()
		);
		assertDateSubclasses(
				ZonedDateTimeJavaType.INSTANCE,
				instant.atZone( ZoneId.of( "America/New_York" ) ),
				instant.toEpochMilli()
		);
	}

	private static <T> void assertDateSubclasses(JavaType<T> javaType, T value, long millis) {
		final var date = javaType.unwrap( value, java.sql.Date.class, null );
		assertEquals( java.sql.Date.class, date.getClass() );
		assertEquals( millis, date.getTime() );
		final var time = javaType.unwrap( value, Time.class, null );
		assertEquals( Time.class, time.getClass() );
		assertEquals( millis, time.getTime() );
		assertEquals( millis, javaType.unwrap( value, Date.class, null ).getTime() );
		assertNull( javaType.unwrap( null, java.sql.Date.class, null ) );
		assertNull( javaType.unwrap( null, Time.class, null ) );
	}

	@Test
	void testJdbcDateDispatch() {
		final var javaType = JdbcDateJavaType.INSTANCE;
		final var date = java.sql.Date.valueOf( LocalDate.of( 2025, 7, 1 ) );
		final var timestamp = javaType.unwrap( date, Timestamp.class, null );
		assertEquals( Timestamp.class, timestamp.getClass() );
		assertEquals( date.getTime(), timestamp.getTime() );
		assertSame( date, javaType.unwrap( date, java.sql.Date.class, null ) );
		assertSame( date, javaType.unwrap( date, Date.class, null ) );
		assertThrows( IllegalArgumentException.class, () -> javaType.unwrap( date, Time.class, null ) );
		assertNull( javaType.unwrap( null, Time.class, null ) );
		assertNull( javaType.unwrap( null, Timestamp.class, null ) );
	}

	@Test
	void testJdbcTimeDispatch() {
		final var javaType = JdbcTimeJavaType.INSTANCE;
		final var time = new Time( 45_296_789L );
		final var timestamp = javaType.unwrap( time, Timestamp.class, null );
		assertEquals( Timestamp.class, timestamp.getClass() );
		assertEquals( time.getTime(), timestamp.getTime() );
		assertSame( time, javaType.unwrap( time, Time.class, null ) );
		assertSame( time, javaType.unwrap( time, Date.class, null ) );
		assertThrows( IllegalArgumentException.class, () -> javaType.unwrap( time, java.sql.Date.class, null ) );
		assertNull( javaType.unwrap( null, java.sql.Date.class, null ) );
		assertNull( javaType.unwrap( null, Timestamp.class, null ) );
	}

	@Test
	void testJdbcTimestampDispatch() {
		final var javaType = JdbcTimestampJavaType.INSTANCE;
		final var timestamp = Timestamp.from( Instant.parse( "2025-07-01T12:34:56.789Z" ) );
		assertEquals( Time.class, javaType.unwrap( timestamp, Time.class, null ).getClass() );
		assertEquals( java.sql.Date.class, javaType.unwrap( timestamp, java.sql.Date.class, null ).getClass() );
		assertSame( timestamp, javaType.unwrap( timestamp, Timestamp.class, null ) );
		assertSame( timestamp, javaType.unwrap( timestamp, Date.class, null ) );
	}

	@Test
	void testOffsetTimeRejectsSqlDate() {
		final var javaType = OffsetTimeJavaType.INSTANCE;
		final var time = OffsetTime.parse( "12:34:56.789+05:30" );
		final var options = mock( WrapperOptions.class );
		assertThrows( IllegalArgumentException.class, () -> javaType.unwrap( time, java.sql.Date.class, options ) );
		assertEquals( time.atDate( LocalDate.EPOCH ).toInstant().toEpochMilli(),
				javaType.unwrap( time, Date.class, options ).getTime() );
		assertNull( javaType.unwrap( null, java.sql.Date.class, options ) );
	}
}
