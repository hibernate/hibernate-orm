/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.internal.util.CharSequenceHelper.subSequence;
import static org.hibernate.type.descriptor.java.DateJavaType.wrapSqlDate;
import static org.hibernate.type.descriptor.java.DateJavaType.wrapSqlTime;

/**
 * Descriptor for {@link Timestamp} handling.
 *
 * @implSpec Unlike most {@link JavaType} implementations, can handle 2 different "domain
 * representations" (most map just a single type): general {@link Date} values in addition
 * to {@link Timestamp} values.  This capability is shared with
 * {@link JdbcDateJavaType} and {@link JdbcTimeJavaType}.
 */
public class JdbcTimestampJavaType extends AbstractTemporalJavaType<Timestamp>
		implements VersionJavaType<Timestamp> {
	public static final JdbcTimestampJavaType INSTANCE = new JdbcTimestampJavaType();

	public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS";

	/**
	 * Intended for use in reading HQL literals and writing SQL literals
	 *
	 * @see #TIMESTAMP_FORMAT
	 */
	public static final DateTimeFormatter LITERAL_FORMATTER = DateTimeFormatter.ofPattern( TIMESTAMP_FORMAT )
			.withZone( ZoneId.from( ZoneOffset.UTC ) );

	private static final DateTimeFormatter ENCODED_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
			.withZone( ZoneId.from( ZoneOffset.UTC ) );

	public JdbcTimestampJavaType() {
		super( Timestamp.class, TimestampMutabilityPlan.INSTANCE );
	}

	@Override
	public Class<Timestamp> getJavaType() {
		return java.sql.Timestamp.class;
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Timestamp;
	}

	@Override
	public Timestamp cast(Object value) {
		return (Timestamp) value;
	}

	@Override
	public boolean areEqual(Timestamp one, Timestamp another) {
		if ( one == another ) {
			return true;
		}
		else if ( one == null || another == null) {
			return false;
		}
		else if ( one.getTime() != another.getTime() ) {
			return false;
		}
		else {
			// both are Timestamps
			final int nn1 = one.getNanos() % 1000000;
			final int nn2 = another.getNanos() % 1000000;
			return nn1 == nn2;
		}
	}

	@Override
	public int extractHashCode(Timestamp value) {
		return Long.hashCode( value.getTime() / 1000 );
	}

	@Override
	public Timestamp coerce(Object value) {
		return wrap( value, null );
	}

	@Override
	public <X> X unwrap(Timestamp value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}

		if ( Time.class.isAssignableFrom( type ) ) {
			return type.cast( wrapSqlTime( value ) );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return type.cast( wrapSqlDate( value ) );
		}

		if ( type.isInstance( value ) ) {
			return type.cast( value );
		}

		if ( LocalDateTime.class.isAssignableFrom( type ) ) {
			final var instant = value.toInstant();
			return type.cast( LocalDateTime.ofInstant( instant, ZoneId.systemDefault() ) );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			final var gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.setTimeInMillis( value.getTime() );
			return type.cast( gregorianCalendar );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return type.cast( value.getTime() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Timestamp wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Timestamp timestamp ) {
			return timestamp;
		}

		if ( value instanceof Date date ) {
			return wrapSqlTimestamp( date );
		}

		if ( value instanceof LocalDateTime localDateTime ) {
			return Timestamp.valueOf( localDateTime );
		}

		if ( value instanceof Long longValue ) {
			return new Timestamp( longValue );
		}

		if ( value instanceof Calendar calendar ) {
			return new Timestamp( calendar.getTimeInMillis() );
		}

		throw unknownWrap( value.getClass() );
	}

	static Timestamp wrapSqlTimestamp(Date date) {
		return new Timestamp( date.getTime() );
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		return switch ( javaType.getTypeName() ) {
			case "java.sql.Date", "java.sql.Timestamp", "java.util.Date", "java.util.Calendar" -> true;
			default -> false;
		};
	}

	@Override
	public String toString(Timestamp value) {
		return LITERAL_FORMATTER.format( value.toInstant() );
	}

	@Override
	public Timestamp fromString(CharSequence string) {
		try {
			final var temporalAccessor = LITERAL_FORMATTER.parse( string );
			final var timestamp = new Timestamp( temporalAccessor.getLong( ChronoField.INSTANT_SECONDS ) * 1000L );
			timestamp.setNanos( temporalAccessor.get( ChronoField.NANO_OF_SECOND ) );
			return timestamp;
		}
		catch ( DateTimeParseException pe) {
			throw new HibernateException( "could not parse timestamp string " + string, pe );
		}
	}

	@Override
	public void appendEncodedString(SqlAppender sb, Timestamp value) {
		ENCODED_FORMATTER.formatTo( value.toInstant(), sb );
	}

	@Override
	public Timestamp fromEncodedString(CharSequence charSequence, int start, int end) {
		try {
			final var temporalAccessor = ENCODED_FORMATTER.parse( subSequence( charSequence, start, end ) );
			if ( temporalAccessor.isSupported( ChronoField.INSTANT_SECONDS ) ) {
				final var timestamp = new Timestamp( temporalAccessor.getLong( ChronoField.INSTANT_SECONDS ) * 1000L );
				timestamp.setNanos( temporalAccessor.get( ChronoField.NANO_OF_SECOND ) );
				return timestamp;
			}
			else {
				return Timestamp.valueOf( LocalDateTime.from( temporalAccessor ) );
			}
		}
		catch ( DateTimeParseException pe) {
			throw new HibernateException( "could not parse timestamp string " + subSequence( charSequence, start, end ), pe );
		}
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( Types.TIMESTAMP );
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) this;
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) JdbcDateJavaType.INSTANCE;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return dialect.getDefaultTimestampPrecision();
	}

	@Override
	public Timestamp next(
			Timestamp current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return seed( length, precision, scale, session );
	}

	@Override
	public Timestamp seed(
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return Timestamp.from( ClockHelper.forPrecision( precision, session ).instant() );
	}


	public static class TimestampMutabilityPlan extends MutableMutabilityPlan<Timestamp> {
		public static final TimestampMutabilityPlan INSTANCE = new TimestampMutabilityPlan();
		@Override
		public Timestamp deepCopyNotNull(Timestamp value) {
			// make sure to get the nanos
			final var copy = new Timestamp( value.getTime() );
			copy.setNanos( value.getNanos() );
			return copy;
		}
	}
}
