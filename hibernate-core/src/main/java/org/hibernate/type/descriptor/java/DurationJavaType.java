/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.math.BigDecimal;
import java.time.Duration;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Descriptor for {@link Duration}, which is represented internally
 * as ({@code long seconds}, {@code int nanoseconds}), approximately
 * 28 decimal digits of precision. This quantity must be stored in
 * the database as a single integer with units of nanoseconds, unless
 * the ANSI SQL {@code interval} type is supported.
 * <p>
 * In practice, the 19 decimal digits of a SQL {@code bigint} are
 * capable of representing six centuries in nanoseconds and are
 * sufficient for many applications. However, by default, we map
 * Java {@link Duration} to SQL {@code numeric(21)} here, which
 * can comfortably represent 60 millennia of nanos.
 *
 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_DURATION_JDBC_TYPE
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class DurationJavaType extends AbstractClassJavaType<Duration> {
	/**
	 * Singleton access
	 */
	public static final DurationJavaType INSTANCE = new DurationJavaType();
	private static final BigDecimal BILLION = BigDecimal.ONE.movePointRight(9);

	public DurationJavaType() {
		super( Duration.class, ImmutableMutabilityPlan.instance() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Duration;
	}

	@Override
	public Duration cast(Object value) {
		return (Duration) value;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getTypeConfiguration()
				.getJdbcTypeRegistry()
				.getDescriptor( context.getPreferredSqlTypeCodeForDuration() );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Duration value) {
		if ( value == null ) {
			return null;
		}
		else {
			final String seconds = String.valueOf( value.getSeconds() );
			final String nanos = String.valueOf( value.getNano() );
			final String zeros = StringHelper.repeat( '0', 9 - nanos.length() );
			return seconds + zeros + nanos;
		}
	}

	@Override
	public Duration fromString(CharSequence string) {
		if ( string == null ) {
			return null;
		}
		else {
			final int cutoff = string.length() - 9;
			return Duration.ofSeconds(
					Long.parseLong( string.subSequence( 0, cutoff ).toString() ),
					Long.parseLong( string.subSequence( cutoff, string.length() ).toString() )
			);
		}
	}

	@Override
	public <X> X unwrap(Duration duration, Class<X> type, WrapperOptions options) {
		if ( duration == null ) {
			return null;
		}

		if ( Duration.class.isAssignableFrom( type ) ) {
			return type.cast( duration );
		}

		if ( BigDecimal.class.isAssignableFrom( type ) ) {
			return type.cast( new BigDecimal( duration.getSeconds() )
					.movePointRight( 9 )
					.add( new BigDecimal( duration.getNano() ) ) );
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( duration.toString() );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return type.cast( duration.toNanos() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Duration wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if (value instanceof Duration duration) {
			return duration;
		}

		if ( value instanceof BigDecimal decimal ) {
			final BigDecimal[] secondsAndNanos = decimal.divideAndRemainder( BILLION );
			return Duration.ofSeconds(
					secondsAndNanos[0].longValueExact(),
					// use intValue() not intValueExact() here, because
					// the database will sometimes produce garbage digits
					// in a floating point multiplication, and we would
					// get an unwanted ArithmeticException
					secondsAndNanos[1].intValue()
			);
		}

		if (value instanceof Double doubleValue) {
			// PostgreSQL returns a Double for datediff(epoch)
			return Duration.ofNanos( doubleValue.longValue() );
		}

		if (value instanceof Long longValue) {
			return Duration.ofNanos( longValue );
		}

		if (value instanceof String string) {
			return Duration.parse( string );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		if ( jdbcType.getDdlTypeCode() == SqlTypes.INTERVAL_SECOND ) {
			// Usually the maximum precision for interval types
			return 18;
		}
		else {
			// 19+9 = 28 digits is the maximum possible Duration
			// precision, but is an unnecessarily large default,
			// except for cosmological applications. Thirty
			// millennia in both timelike directions should be
			// sufficient time for most businesses!
			return Math.min( 21, dialect.getDefaultDecimalPrecision() );
		}
	}

	@Override
	public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
		return jdbcType.getDdlTypeCode() == SqlTypes.INTERVAL_SECOND
				? dialect.getDefaultIntervalSecondScale()
				: 0; // For non-interval types, we use the type numeric(21)

	}
}
