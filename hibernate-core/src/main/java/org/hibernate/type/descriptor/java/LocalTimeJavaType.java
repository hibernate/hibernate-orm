/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Java type descriptor for the {@link LocalTime} type.
 *
 * @author Steve Ebersole
 */
public class LocalTimeJavaType extends AbstractTemporalJavaType<LocalTime> {
	/**
	 * Singleton access
	 */
	public static final LocalTimeJavaType INSTANCE = new LocalTimeJavaType();
	private static final String LOCAL_DATE_FORMAT = "MM/dd/yyyy";
	private static final String LOCAL_TIME_FORMAT = "HH:mm:ss.SSS";
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(LOCAL_DATE_FORMAT + " " + LOCAL_TIME_FORMAT);
	private static final  DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
		.appendPattern("MM/dd/yyyy HH:mm:ss")
		.appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, true)
		.toFormatter();

	public LocalTimeJavaType() {
		super( LocalTime.class, ImmutableMutabilityPlan.instance() );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIME;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		if ( context.isPreferJavaTimeJdbcTypesEnabled() ) {
			return context.getJdbcType( SqlTypes.LOCAL_TIME );
		}
		return context.getJdbcType( Types.TIME );
	}

	@Override
	protected <X> TemporalJavaType<X> forTimePrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (TemporalJavaType<X>) this;
	}

	@Override
	public String toString(LocalTime value) {
		return DateTimeFormatter.ISO_LOCAL_TIME.format( value );
	}

	@Override
	public LocalTime fromString(CharSequence string) {
		return LocalTime.from( DateTimeFormatter.ISO_LOCAL_TIME.parse( string ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(LocalTime value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalTime.class.isAssignableFrom( type ) ) {
			return (X) value;
		}

		if ( Time.class.isAssignableFrom( type ) ) {
			final Time time = Time.valueOf( value );
			if ( value.getNano() == 0 ) {
				return (X) time;
			}
			// Preserve milliseconds, which java.sql.Time supports
			return (X) new Time( time.getTime() + DateTimeUtils.roundToPrecision( value.getNano(), 3 ) / 1000000 );
		}

		// Oracle documentation says to set the Date to January 1, 1970 when convert from
		// a LocalTime to a Calendar.  IMO the same should hold true for converting to all
		// the legacy Date/Time types...


		final ZonedDateTime zonedDateTime = value.atDate( LocalDate.of( 1970, 1, 1 ) ).atZone( ZoneId.systemDefault() );

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( zonedDateTime );
		}

		final Instant instant = zonedDateTime.toInstant();

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			return (X) Timestamp.from( instant );
		}

		if ( Date.class.equals( type ) ) {
			return (X) Date.from( instant );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( instant.toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> LocalTime wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if (value instanceof LocalTime) {
			return (LocalTime) value;
		}

		if (value instanceof Time) {
			final Time time = (Time) value;
			var date = DATE_FORMATTER.format(time);
			return LocalTime.parse(date, DATE_TIME_FORMATTER);
		}

		if (value instanceof Timestamp) {
			final Timestamp ts = (Timestamp) value;
			return LocalDateTime.ofInstant( ts.toInstant(), ZoneId.systemDefault() ).toLocalTime();
		}

		if (value instanceof Long) {
			final Instant instant = Instant.ofEpochMilli( (Long) value );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() ).toLocalTime();
		}

		if (value instanceof Calendar) {
			final Calendar calendar = (Calendar) value;
			return LocalDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() ).toLocalTime();
		}

		if (value instanceof Date) {
			final Date ts = (Date) value;
			final Instant instant = Instant.ofEpochMilli( ts.getTime() );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() ).toLocalTime();
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		switch ( javaType.getTypeName() ) {
			case "java.sql.Time":
				return true;
			default:
				return false;
		}
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return dialect.getDefaultTimestampPrecision();
	}

}
