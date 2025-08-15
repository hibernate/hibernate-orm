/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.hibernate.internal.util.CharSequenceHelper.subSequence;

/**
 * Java type descriptor for the {@link OffsetDateTime} type.
 *
 * @author Steve Ebersole
 */
public class OffsetDateTimeJavaType extends AbstractTemporalJavaType<OffsetDateTime>
		implements VersionJavaType<OffsetDateTime> {
	/**
	 * Singleton access
	 */
	public static final OffsetDateTimeJavaType INSTANCE = new OffsetDateTimeJavaType();

	private static final DateTimeFormatter PARSE_FORMATTER;
	static {
		PARSE_FORMATTER = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.append(ISO_LOCAL_DATE_TIME)
				.optionalStart()
				.parseLenient()
				.appendOffset( "+HH:MM:ss", "Z" )
				.parseStrict()
				.toFormatter();
	}

	public OffsetDateTimeJavaType() {
		super( OffsetDateTime.class, ImmutableMutabilityPlan.instance(), OffsetDateTime.timeLineOrder() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof OffsetDateTime;
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators stdIndicators) {
		if ( stdIndicators.isPreferJavaTimeJdbcTypesEnabled() ) {
			return stdIndicators.getJdbcType( SqlTypes.OFFSET_DATE_TIME );
		}
		return stdIndicators.getJdbcType( stdIndicators.getDefaultZonedTimestampSqlType() );
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) this;
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(OffsetDateTime value) {
		return ISO_OFFSET_DATE_TIME.format( value );
	}

	@Override
	public OffsetDateTime fromString(CharSequence string) {
		return OffsetDateTime.from( ISO_OFFSET_DATE_TIME.parse( string ) );
	}

	@Override
	public OffsetDateTime fromEncodedString(CharSequence charSequence, int start, int end) {
		try {
			final TemporalAccessor temporalAccessor = PARSE_FORMATTER.parse( subSequence( charSequence, start, end ) );
			if ( temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
				return OffsetDateTime.from( temporalAccessor );
			}
			else {
				// For databases that don't have timezone support, we encode timestamps at UTC, so allow parsing that as well
				return LocalDateTime.from( temporalAccessor ).atOffset( ZoneOffset.UTC );
			}
		}
		catch ( DateTimeParseException pe) {
			throw new HibernateException( "could not parse timestamp string " + subSequence( charSequence, start, end ), pe );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(OffsetDateTime offsetDateTime, Class<X> type, WrapperOptions options) {
		if ( offsetDateTime == null ) {
			return null;
		}

		if ( OffsetDateTime.class.isAssignableFrom( type ) ) {
			return (X) offsetDateTime;
		}

		if ( ZonedDateTime.class.isAssignableFrom( type ) ) {
			return (X) offsetDateTime.toZonedDateTime();
		}

		if ( Instant.class.isAssignableFrom( type ) ) {
			return (X) offsetDateTime.toInstant();
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( offsetDateTime.toZonedDateTime() );
		}

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			/*
			 * This works around two bugs:
			 * - HHH-13266 (JDK-8061577): around and before 1900,
			 * the number of milliseconds since the epoch does not mean the same thing
			 * for java.util and java.time, so conversion must be done using the year, month, day, hour, etc.
			 * - HHH-13379 (JDK-4312621): after 1908 (approximately),
			 * Daylight Saving Time introduces ambiguity in the year/month/day/hour/etc representation once a year
			 * (on DST end), so conversion must be done using the number of milliseconds since the epoch.
			 * - around 1905, both methods are equally valid, so we don't really care which one is used.
			 */
			if ( offsetDateTime.getYear() < 1905 ) {
				return (X) Timestamp.valueOf(
						offsetDateTime.atZoneSameInstant( ZoneId.systemDefault() ).toLocalDateTime()
				);
			}
			else {
				return (X) Timestamp.from( offsetDateTime.toInstant() );
			}
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Date.from( offsetDateTime.toInstant() );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Time.from( offsetDateTime.toInstant() );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			return (X) Date.from( offsetDateTime.toInstant() );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( offsetDateTime.toInstant().toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> OffsetDateTime wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if (value instanceof OffsetDateTime offsetDateTime) {
			return offsetDateTime;
		}

		if (value instanceof ZonedDateTime zonedDateTime) {
			return OffsetDateTime.of( zonedDateTime.toLocalDateTime(), zonedDateTime.getOffset() );
		}

		if (value instanceof Instant instant) {
			return instant.atOffset( ZoneOffset.UTC );
		}

		if (value instanceof Timestamp timestamp) {
			return DateTimeUtils.toInstant( timestamp ).atZone( ZoneId.systemDefault() ).toOffsetDateTime();
		}

		if (value instanceof Date date) {
			return OffsetDateTime.ofInstant( date.toInstant(), ZoneId.systemDefault() );
		}

		if (value instanceof Long longValue) {
			return OffsetDateTime.ofInstant( Instant.ofEpochMilli( longValue ), ZoneId.systemDefault() );
		}

		if (value instanceof Calendar calendar) {
			return OffsetDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return dialect.getDefaultTimestampPrecision();
	}

	@Override
	public OffsetDateTime seed(
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return OffsetDateTime.now( ClockHelper.forPrecision( precision, session ) );
	}

	@Override
	public OffsetDateTime next(
			OffsetDateTime current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return OffsetDateTime.now( ClockHelper.forPrecision( precision, session ) );
	}

}
