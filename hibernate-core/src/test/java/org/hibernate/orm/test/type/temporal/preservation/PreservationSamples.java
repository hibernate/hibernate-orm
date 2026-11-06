package org.hibernate.orm.test.type.temporal.preservation;

import java.time.*;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;
import java.util.*;

/// Reviewed inputs and independent expected transformations. No Hibernate
/// converter, decomposition or precision helper participates in the oracle.
///
/// @author Steve Ebersole
final class PreservationSamples {
	record Data<T>(String id, T value, boolean rounding, boolean rejected) {
		Data(String id, T value, boolean rounding) { this( id, value, rounding, false ); }
	}

	static <T> List<Data<T>> samples(Class<T> type, boolean full) {
		final List<Object> values = new ArrayList<>();
		if ( type == LocalDate.class ) {
			values.add( LocalDate.of( 2024, 2, 29 ) );
			if ( full ) {
				for ( String date : List.of(
						"1500-01-01", "1582-10-04", "1582-10-10", "1582-10-15", "1600-01-01",
						"1892-01-01",
						"1900-01-01",
						"1904-12-31",
						"1905-01-01",
						"1969-12-31",
						"1970-01-01" ) ) {
					values.add( LocalDate.parse( date ) );
				}
			}
		}
		else if ( type == LocalTime.class ) {
			values.add( LocalTime.parse( "23:59:58.123456" ) );
			if ( full ) {
				values.add( LocalTime.MIDNIGHT );
				values.add( LocalTime.parse( "23:59:59.999999900" ) );
			}
		}
		else if ( type == LocalDateTime.class ) {
			values.add( LocalDateTime.parse( "2024-02-29T23:59:58.123456" ) );
			if ( full ) {
				if ( type == ZonedDateTime.class ) values.add( LocalDateTime.parse( "2026-07-01T12:34:56" ).atZone( ZoneId.of( "GMT+05:30" ) ) );
				for ( String text : List.of(
						"1500-01-01T12:34:56", "1582-10-10T12:34:56", "1600-01-01T12:34:56",
						"1892-01-01T12:34:56",
						"1900-01-01T12:34:56",
						"1904-12-31T23:59:59.123456",
						"1905-01-01T00:00:00",
						"2026-03-08T02:30:00",
						"2026-11-01T01:30:00",
						"2024-02-29T23:59:59.999999900" ) ) {
					values.add( LocalDateTime.parse( text ) );
				}
			}
		}
		else if ( type == OffsetTime.class ) {
			values.add( OffsetTime.parse( "00:15:16.123456+05:30" ) );
			if ( full ) {
				if ( type == ZonedDateTime.class ) values.add( LocalDateTime.parse( "2026-07-01T12:34:56" ).atZone( ZoneId.of( "GMT+05:30" ) ) );
				for ( String text : List.of(
						"23:45:16.654321-03:00",
						"12:34:56.123456+05:30:45",
						"00:00:00+18:00",
						"23:59:59-18:00",
						"12:34:56+15:59:59",
						"12:34:56-15:59:59",
						"12:34:56+16:00",
						"12:34:56-16:00",
						"23:59:59.999999900Z" ) ) {
					values.add( OffsetTime.parse( text ) );
				}
			}
		}
		else {
			for ( String text : List.of(
					"2026-11-01T01:30:16.123456-04:00[America/New_York]",
					"2026-11-01T01:30:16.654321-05:00[America/New_York]",
					"2026-07-01T12:34:56.123456-04:00[America/New_York]", "2026-01-15T12:34:56.123456-05:00[America/New_York]" ) ) {
				addTimestamp( values, type, ZonedDateTime.parse( text ) );
			}
			addTimestamp( values, type, ZonedDateTime.parse( "1500-01-01T00:00Z" ) );
			if ( full ) {
				if ( type == ZonedDateTime.class ) values.add( LocalDateTime.parse( "2026-07-01T12:34:56" ).atZone( ZoneId.of( "GMT+05:30" ) ) );
				for ( String text : List.of(
						"1969-12-31T23:59:58Z",
						"1970-01-01T00:00:00Z",
						"1904-12-31T23:59:59.123456Z",
						"1905-01-01T00:00:00Z",
						"2026-03-08T01:59:59.600-05:00[America/New_York]",
						"2026-11-01T01:59:59.600-04:00[America/New_York]",
						"2024-02-29T23:59:59.999999900Z",
						"2024-02-29T12:34:56.499999999Z",
						"2024-02-29T12:34:56+05:30:45",
						"2024-02-29T12:34:56+18:00",
						"2024-02-29T12:34:56-18:00",
						"2024-02-29T12:34:56+15:59:59",
						"2024-02-29T12:34:56+16:00" ) ) {
					addTimestamp( values, type, ZonedDateTime.parse( text ) );
				}
			}
		}
		final List<Data<T>> result = new ArrayList<>();
		for ( int i = 0; i < values.size(); i++ ) {
			final T value = type.cast( values.get( i ) );
			final int nano = value instanceof Temporal temporal && temporal.isSupported( ChronoField.NANO_OF_SECOND ) ? temporal.get( ChronoField.NANO_OF_SECOND ) : 0;
			result.add( new Data<>( "sample-" + i, value, nano % 1000 != 0 || nano == 600000000 ) );
		}
		return result;
	}

	private static void addTimestamp(List<Object> values, Class<?> type, ZonedDateTime value) {
		values.add( type == Instant.class ? value.toInstant() : type == OffsetDateTime.class ? value.toOffsetDateTime() : value );
	}

	static Object adjust(Object value, String target) {
		if ( value == null || target == null || target.equals( "<none>" ) ) {
			return value;
		}
		final ZoneId zone = ZoneId.of( target );
		if ( value instanceof OffsetTime time ) {
			return time.withOffsetSameInstant( (ZoneOffset) zone );
		}
		if ( value instanceof OffsetDateTime time ) {
			return time.toInstant().atZone( zone ).toOffsetDateTime();
		}
		if ( value instanceof ZonedDateTime time ) {
			return time.toInstant().atZone( zone );
		}
		throw new IllegalArgumentException( "Inapplicable adjustment: " + value.getClass() );
	}

	static Object expected(Object input, Mapping<?> mapping, PreservationEnvironment environment) {
		Object adjusted = adjust( input, mapping.adjustment() );
		if ( adjusted == null || adjusted instanceof LocalDate ) {
			return adjusted;
		}
		final ZoneOffset storedTimeOffset = adjusted instanceof OffsetTime time ? time.getOffset() : null;
		final boolean utcTime = adjusted instanceof OffsetTime && (mapping.column() || mapping.adjusted());
		if ( utcTime ) {
			adjusted = ((OffsetTime) adjusted).withOffsetSameInstant( ZoneOffset.UTC );
		}
		if ( mapping.column() && adjusted instanceof ZonedDateTime time ) {
			adjusted = time.toOffsetDateTime().toZonedDateTime();
		}
		Temporal temporal = (Temporal) adjusted;
		int nano = temporal.get( ChronoField.NANO_OF_SECOND );
		long quantum = (long) Math.pow( 10, 9 - mapping.precision() );
		// pgJDBC encodes timestamp inputs at microsecond precision before the column rounds.
		if ( environment.pg() && !(adjusted instanceof LocalTime) && !(adjusted instanceof OffsetTime) ) {
			nano = (int) ((nano + 500L) / 1000 * 1000);
		}
		long rounded = ((nano + quantum / 2) / quantum) * quantum;
		if ( environment.platform().equals( "h2" ) && (adjusted instanceof LocalTime || adjusted instanceof OffsetTime) ) {
			// Pinned H2 Value.convertToTime[TimeZone] clamps a rounding carry at the last
			// representable quantum of the day (DateTimeUtils.convertScale with NANOS_PER_DAY).
			long nanos = temporal.getLong( ChronoField.NANO_OF_DAY ) - nano + rounded;
			temporal = temporal.with( ChronoField.NANO_OF_DAY, Math.min( nanos, 86400000000000L - quantum ) );
		}
		else {
			temporal = temporal.with( ChronoField.NANO_OF_SECOND, 0 ).plus( rounded, java.time.temporal.ChronoUnit.NANOS );
		}
		Object result = temporal;
		if ( utcTime ) {
			result = ((OffsetTime) result).withOffsetSameInstant( storedTimeOffset );
		}
		return adjust( result, mapping.adjustment() );
	}
}
