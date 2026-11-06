package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.*;
import java.time.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import static org.assertj.core.api.Assertions.assertThat;

/// Physical encoding oracles use JDBC directly and java.time operations only;
/// no Hibernate binder, extractor or composite implementation supplies expectations.
///
/// @author Steve Ebersole
final class PreservationJdbcOracle {
	record Storage(String table, String sql, boolean column, boolean withZone, boolean oracle, boolean pg, boolean sqlTimestamp, Class<?> domain) {}
	private static Calendar utc() { return Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ); }

	static void inspect(Connection connection, Storage storage, int id, Object input, Mapping<?> mapping, Object expected) throws Exception {
		try ( var statement = connection.createStatement(); var result = statement.executeQuery(
				"select temporal_value" + (storage.column ? ", offset_seconds" : "") + " from " + storage.table + " where id=" + id ) ) {
			assertThat( result.next() ).isTrue();
			Object value;
			if ( storage.domain == LocalDate.class ) value = result.getObject( 1, LocalDate.class );
			else if ( storage.domain == LocalTime.class ) value = storage.oracle
					? result.getObject( 1, LocalDateTime.class ).toLocalTime() : localTime( result );
			else if ( storage.domain == LocalDateTime.class ) value = result.getObject( 1, LocalDateTime.class );
			else if ( storage.domain == OffsetTime.class ) {
				OffsetTime time = storage.withZone ? result.getObject( 1, OffsetTime.class ) : localTime( result ).atOffset( ZoneOffset.UTC );
				if ( storage.pg && storage.withZone ) {
					// A plain Statement returns textual timetz, avoiding pgJDBC's binary getString conversion.
					String text = result.getString( 1 );
					if ( text.startsWith( "24:00:00" ) ) text = "00:00:00" + text.substring( 8 );
					if ( text.matches( ".*[+-][0-9]{2}" ) ) text += ":00";
					time = OffsetTime.parse( text );
				}
				if ( storage.column ) time = time.withOffsetSameInstant( offset( result, input, mapping ) );
				value = PreservationSamples.adjust( time, mapping.adjustment() );
			}
			else if ( storage.domain == ZonedDateTime.class && !storage.column && !mapping.adjusted() ) value = result.getObject( 1, ZonedDateTime.class );
			else if ( storage.domain == OffsetDateTime.class && !storage.column && !mapping.adjusted() ) value = result.getObject( 1, OffsetDateTime.class );
			else {
				final Instant instant;
				if ( storage.sqlTimestamp ) {
					instant = result.getTimestamp( 1, utc() ).toInstant();
					final LocalDateTime fields = storage.withZone ? result.getObject( 1, OffsetDateTime.class ).withOffsetSameInstant( ZoneOffset.UTC ).toLocalDateTime()
							: result.getObject( 1, LocalDateTime.class );
					final Object adjusted = PreservationSamples.adjust( input, mapping.adjustment() );
					final Instant rawInstant = instant( adjusted );
					if ( rawInstant.getNano() % (long) Math.pow( 10, 9 - mapping.precision() ) == 0 ) {
						AbstractPreservationTests.assertValue( fields, encodedFields( rawInstant, storage ) );
					}
				}
				else instant = result.getObject( 1, OffsetDateTime.class ).toInstant();
				if ( storage.domain == Instant.class ) value = instant;
				else {
					final ZoneOffset offset = storage.column ? offset( result, input, mapping ) : ZoneOffset.UTC;
					value = storage.domain == ZonedDateTime.class ? instant.atZone( offset ) : instant.atOffset( offset );
					value = PreservationSamples.adjust( value, mapping.adjustment() );
				}
			}
			AbstractPreservationTests.assertValue( value, expected );
		}
	}
	private static LocalTime localTime(ResultSet result) throws SQLException {
		final String text = result.getString( 1 );
		return text.startsWith( "24:00:00" ) ? LocalTime.MIDNIGHT : result.getObject( 1, LocalTime.class );
	}
	private static ZoneOffset offset(ResultSet result, Object input, Mapping<?> mapping) throws SQLException {
		final Object adjusted = PreservationSamples.adjust( input, mapping.adjustment() );
		final ZoneOffset expected = adjusted instanceof OffsetTime t ? t.getOffset() : adjusted instanceof OffsetDateTime t ? t.getOffset() : ((ZonedDateTime) adjusted).getOffset();
		final int seconds = result.getInt( 2 );
		assertThat( result.wasNull() ).isFalse();
		assertThat( seconds ).isEqualTo( expected.getTotalSeconds() );
		return ZoneOffset.ofTotalSeconds( seconds );
	}
	private static Instant instant(Object value) {
		return value instanceof Instant i ? i : value instanceof OffsetDateTime t ? t.toInstant() : ((ZonedDateTime) value).toInstant();
	}
	static void insert(Connection connection, Storage storage, int id, Object input, Mapping<?> mapping) throws Exception {
		final Object adjusted = PreservationSamples.adjust( input, mapping.adjustment() );
		try ( var statement = connection.prepareStatement( "insert into " + storage.table
				+ " (id, writerProcess, temporal_value" + (storage.column ? ", offset_seconds" : "") + ") values (?, 0, ?" + (storage.column ? ", ?" : "") + ")" ) ) {
			statement.setInt( 1, id );
			if ( adjusted instanceof LocalDate date ) statement.setObject( 2, date, Types.DATE );
			else if ( adjusted instanceof LocalTime time ) statement.setObject( 2, storage.oracle ? time.atDate( LocalDate.of( 1970, 1, 1 ) ) : time );
			else if ( adjusted instanceof LocalDateTime timestamp ) statement.setObject( 2, timestamp, Types.TIMESTAMP );
			else if ( adjusted instanceof OffsetTime time ) {
				final OffsetTime primary = storage.column || mapping.adjusted() ? time.withOffsetSameInstant( ZoneOffset.UTC ) : time;
				if ( storage.withZone ) {
					if ( storage.pg ) statement.setObject( 2, primary );
					else statement.setObject( 2, primary, Types.TIME_WITH_TIMEZONE );
				}
				else statement.setObject( 2, primary.toLocalTime() );
				if ( storage.column ) statement.setInt( 3, time.getOffset().getTotalSeconds() );
			}
			else {
				final Instant instant = instant( adjusted );
				if ( storage.sqlTimestamp ) {
					// Write the independently specified fields through typed Java Time access,
					// rather than repeating Hibernate's Timestamp/Calendar binding operation.
					final LocalDateTime fields = encodedFields( instant, storage );
					statement.setObject( 2, storage.withZone ? fields.atOffset( ZoneOffset.UTC ) : fields,
							storage.withZone ? Types.TIMESTAMP_WITH_TIMEZONE : Types.TIMESTAMP );
				}
				else if ( !storage.withZone ) statement.setObject( 2, instant.atOffset( ZoneOffset.UTC ).toLocalDateTime(), Types.TIMESTAMP );
				else if ( storage.column || mapping.adjusted() || adjusted instanceof Instant ) statement.setObject( 2, instant.atOffset( ZoneOffset.UTC ), Types.TIMESTAMP_WITH_TIMEZONE );
				else statement.setObject( 2, adjusted, Types.TIMESTAMP_WITH_TIMEZONE );
				if ( storage.column ) statement.setInt( 3, (adjusted instanceof OffsetDateTime t ? t.getOffset() : ((ZonedDateTime) adjusted).getOffset()).getTotalSeconds() );
			}
			assertThat( statement.executeUpdate() ).isEqualTo( 1 );
		}
	}
	private static LocalDateTime encodedFields(Instant instant, Storage storage) {
		if ( !storage.oracle && !storage.pg ) return instant.atOffset( ZoneOffset.UTC ).toLocalDateTime();
		final var calendar = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) );
		calendar.setTimeInMillis( instant.toEpochMilli() );
		final int year = calendar.get( Calendar.ERA ) == GregorianCalendar.BC ? 1 - calendar.get( Calendar.YEAR ) : calendar.get( Calendar.YEAR );
		return LocalDateTime.of( year, calendar.get( Calendar.MONTH ) + 1, calendar.get( Calendar.DAY_OF_MONTH ),
				calendar.get( Calendar.HOUR_OF_DAY ), calendar.get( Calendar.MINUTE ), calendar.get( Calendar.SECOND ), instant.getNano() );
	}
}
