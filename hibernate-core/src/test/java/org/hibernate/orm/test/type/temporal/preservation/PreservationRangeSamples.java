package org.hibernate.orm.test.type.temporal.preservation;

import java.time.*;
import java.util.*;

/// Independent finite SQL and carrier boundary cases. Calendar-based encodings
/// have their own boundary instants; typed ISO encodings use proleptic fields.
///
/// @author Steve Ebersole
final class PreservationRangeSamples {
	static <T> List<PreservationSamples.Data<T>> samples(Class<T> type, Mapping<T> mapping,
			PreservationEnvironment environment, boolean sqlTimestamp) {
		if ( type == LocalTime.class || type == OffsetTime.class ) return List.of();
		final List<PreservationSamples.Data<T>> result = new ArrayList<>();
		final LocalDate min = environment.oracle() ? LocalDate.of( -4711, 1, 1 )
				: environment.pg() ? LocalDate.of( -4712, 1, 1 ) : LocalDate.MIN;
		final LocalDate max = environment.oracle() ? LocalDate.of( 9999, 12, 31 )
				: environment.pg() ? LocalDate.of( type == LocalDate.class ? 5874897 : 294276, 12, 31 ) : LocalDate.MAX;
		if ( type == LocalDate.class ) {
			add( result, type, "range-min", min, false ); add( result, type, "range-max", max, false );
			if ( min != LocalDate.MIN ) add( result, type, "below-range", min.minusDays( 1 ), true );
			if ( max != LocalDate.MAX ) add( result, type, "above-range", max.plusDays( 1 ), true );
			if ( environment.pg() || environment.oracle() ) {
				add( result, type, "java-min-finite-only", LocalDate.MIN, true ); add( result, type, "java-max-finite-only", LocalDate.MAX, true );
			}
			return result;
		}
		final long quantum = (long) Math.pow( 10, 9 - mapping.precision() );
		if ( type == LocalDateTime.class ) {
			add( result, type, "range-min", min.atStartOfDay(), false );
			add( result, type, "range-max", max.atTime( 23, 59, 59, (int) (1000000000 - quantum) ), false );
			if ( !min.equals( LocalDate.MIN ) ) add( result, type, "below-range", min.atStartOfDay().minusNanos( quantum ), true );
			if ( !max.equals( LocalDate.MAX ) ) add( result, type, "above-range", max.plusDays( 1 ).atStartOfDay(), true );
			if ( environment.pg() || environment.oracle() ) {
				add( result, type, "java-min-finite-only", LocalDateTime.MIN, true ); add( result, type, "java-max-finite-only", LocalDateTime.MAX, true );
			}
			return result;
		}
		Instant low = min.atStartOfDay().toInstant( ZoneOffset.UTC );
		Instant high = max.atTime( 23, 59, 59, (int) (1000000000 - quantum) ).toInstant( ZoneOffset.UTC );
		if ( sqlTimestamp ) {
			if ( environment.pg() || environment.oracle() ) low = calendarInstant( min );
			else {
				low = Instant.ofEpochSecond( Long.MIN_VALUE / 1000 );
				high = Instant.ofEpochMilli( Long.MAX_VALUE ).plusNanos( 999000 );
				high = high.with( java.time.temporal.ChronoField.NANO_OF_SECOND, high.getNano() / quantum * quantum );
			}
		}
		if ( mapping.adjusted() && type != Instant.class ) {
			final ZoneId target = ZoneId.of( mapping.adjustment() );
			final Instant targetLow = LocalDateTime.MIN.atZone( target ).toInstant();
			final Instant targetHigh = LocalDateTime.MAX.atZone( target ).toInstant();
			if ( low.isBefore( targetLow ) ) low = targetLow;
			if ( high.isAfter( targetHigh ) ) high = targetHigh;
			high = high.with( java.time.temporal.ChronoField.NANO_OF_SECOND, high.getNano() / quantum * quantum );
		}
		addInstant( result, type, "range-min", low, false, mapping.column() || mapping.adjusted() );
		addInstant( result, type, "range-max", high, false, mapping.column() || mapping.adjusted() );
		addInstant( result, type, "below-range", low.minusNanos( quantum ), true, mapping.column() || mapping.adjusted() );
		addInstant( result, type, "above-range", high.plusNanos( quantum ), true, mapping.column() || mapping.adjusted() );
		if ( type == Instant.class ) {
			add( result, type, "java-min-finite-only", Instant.MIN, true ); add( result, type, "java-max-finite-only", Instant.MAX, true );
		}
		return result;
	}
	private static Instant calendarInstant(LocalDate date) {
		final var calendar = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) );
		calendar.clear();
		calendar.set( Calendar.ERA, date.getYear() <= 0 ? GregorianCalendar.BC : GregorianCalendar.AD );
		calendar.set( Calendar.YEAR, date.getYear() <= 0 ? 1 - date.getYear() : date.getYear() );
		calendar.set( Calendar.MONTH, date.getMonthValue() - 1 ); calendar.set( Calendar.DAY_OF_MONTH, date.getDayOfMonth() );
		return Instant.ofEpochMilli( calendar.getTimeInMillis() );
	}
	private static <T> void addInstant(List<PreservationSamples.Data<T>> result, Class<T> type, String id, Instant instant, boolean rejected, boolean utcCarrier) {
		if ( type == Instant.class ) { add( result, type, id, instant, rejected ); return; }
		OffsetDateTime value;
		try { value = instant.atOffset( ZoneOffset.UTC ); }
		catch (DateTimeException ex) {
			if ( !utcCarrier ) return; // No Java value exists beyond the native local-field boundary.
			value = instant.atOffset( instant.isAfter( Instant.EPOCH ) ? ZoneOffset.ofHours( -18 ) : ZoneOffset.ofHours( 18 ) );
		}
		add( result, type, id, type == ZonedDateTime.class ? value.toZonedDateTime() : value, rejected );
	}
	private static <T> void add(List<PreservationSamples.Data<T>> result, Class<T> type, String id, Object value, boolean rejected) {
		result.add( new PreservationSamples.Data<>( id, type.cast( value ), false, rejected ) );
	}
}
