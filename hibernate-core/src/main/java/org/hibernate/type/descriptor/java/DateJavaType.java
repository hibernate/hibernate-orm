/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Date} handling.
 *
 * @author Steve Ebersole
 */
public class DateJavaType extends AbstractTemporalJavaType<Date> implements VersionJavaType<Date> {
	public static final DateJavaType INSTANCE = new DateJavaType();
	private final @SuppressWarnings("deprecation") TemporalType precision;

	public static class DateMutabilityPlan extends MutableMutabilityPlan<Date> {
		@SuppressWarnings("deprecation")
		public static final DateMutabilityPlan INSTANCE =
				new DateMutabilityPlan( TemporalType.TIMESTAMP );

		private final @SuppressWarnings("deprecation") TemporalType precision;

		public DateMutabilityPlan(@SuppressWarnings("deprecation") TemporalType precision) {
			this.precision = precision;
		}

		@Override
		public Date deepCopyNotNull(Date value) {
			if ( value instanceof java.sql.Timestamp timestamp ) {
				return JdbcTimestampJavaType.TimestampMutabilityPlan.INSTANCE.deepCopyNotNull( timestamp );
			}
			else if ( value instanceof java.sql.Date date ) {
				return JdbcDateJavaType.DateMutabilityPlan.INSTANCE.deepCopyNotNull( date );
			}
			else if ( value instanceof java.sql.Time time ) {
				return JdbcTimeJavaType.TimeMutabilityPlan.INSTANCE.deepCopyNotNull( time );
			}
			else {
				return switch ( precision ) {
					case TIMESTAMP -> toTimestamp( value );
					case DATE -> toDate( value );
					case TIME -> toTime( value );
				};
			}
		}
	}

	@SuppressWarnings("deprecation")
	public DateJavaType() {
		super( Date.class, DateMutabilityPlan.INSTANCE );
		this.precision = TemporalType.TIMESTAMP;
	}

	/**
	 * A {@link Date} may be used to represent a date, time, or timestamp,
	 * each of which have different semantics at the Java level. Therefore,
	 * we distinguish these usages based on the given {@code TemporalType}.
	 */
	private DateJavaType(@SuppressWarnings("deprecation") TemporalType precision) {
		super( Date.class, new DateMutabilityPlan( precision ) );
		this.precision = precision;
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Date;
	}

	@Override
	public Date cast(Object value) {
		return (Date) value;
	}

	@Override
	public @SuppressWarnings("deprecation") TemporalType getPrecision() {
		return precision;
	}

	@Override
	public TemporalJavaType<Date> resolveTypeForPrecision(
			@SuppressWarnings("deprecation") TemporalType precision,
			TypeConfiguration typeConfiguration) {
		return precision == null ? this : new DateJavaType( precision );
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return switch ( precision ) {
			case TIMESTAMP -> JdbcTimestampJavaType.INSTANCE.getDefaultSqlPrecision( dialect, jdbcType );
			case DATE -> JdbcDateJavaType.INSTANCE.getDefaultSqlPrecision( dialect, jdbcType );
			case TIME -> JdbcTimeJavaType.INSTANCE.getDefaultSqlPrecision( dialect, jdbcType );
		};
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( switch ( precision ) {
			case TIMESTAMP -> Types.TIMESTAMP;
			case DATE -> Types.DATE;
			case TIME -> Types.TIME;
		} );
	}

	@Override
	public String toString(Date value) {
//		return JdbcTimestampJavaType.LITERAL_FORMATTER.format( value.toInstant() );
		return switch ( precision ) {
			case TIMESTAMP -> JdbcTimestampJavaType.INSTANCE.toString( toTimestamp( value ) );
			case DATE -> JdbcDateJavaType.INSTANCE.toString( toDate( value ) );
			case TIME -> JdbcTimeJavaType.INSTANCE.toString( toTime( value ) );
		};
	}

	@Override
	public Date fromString(CharSequence string) {
		return switch ( precision ) {
			case TIMESTAMP -> JdbcTimestampJavaType.INSTANCE.fromString( string );
			case DATE -> JdbcDateJavaType.INSTANCE.fromString( string );
			case TIME -> JdbcTimeJavaType.INSTANCE.fromString( string );
		};
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another) {
			return true;
		}
		return one != null && another != null
			&& switch ( precision ) {
				case DATE -> JdbcDateJavaType.INSTANCE.areEqual( toDate( one ), toDate( another ) );
				case TIME -> JdbcTimeJavaType.INSTANCE.areEqual( toTime( one ), toTime( another ) );
				case TIMESTAMP ->
						// emulate legacy behavior (good or not)
						one instanceof Timestamp timestamp && another instanceof Timestamp anotherTimestamp
							? JdbcTimestampJavaType.INSTANCE.areEqual( timestamp, anotherTimestamp )
							: one.getTime() == another.getTime();
			};
	}

	@Override @SuppressWarnings("deprecation")
	public int extractHashCode(Date value) {
		var calendar = Calendar.getInstance();
		calendar.setTime( value );
		int hashCode = 1;
		if ( precision == TemporalType.TIMESTAMP ) {
			hashCode = 31 * hashCode + calendar.get(Calendar.MILLISECOND);
		}
		if ( precision != TemporalType.DATE ) {
			hashCode = 31 * hashCode + calendar.get(Calendar.SECOND);
			hashCode = 31 * hashCode + calendar.get(Calendar.MINUTE);
			hashCode = 31 * hashCode + calendar.get(Calendar.HOUR_OF_DAY);
		}
		if ( precision != TemporalType.TIME ) {
			hashCode = 31 * hashCode + calendar.get(Calendar.DAY_OF_MONTH);
			hashCode = 31 * hashCode + calendar.get(Calendar.MONTH);
			hashCode = 31 * hashCode + calendar.get(Calendar.YEAR);
		}
		return hashCode;
	}

	@Override
	public <X> X unwrap(Date value, Class<X> type, WrapperOptions options) {
		return switch ( precision ) {
			case TIMESTAMP -> JdbcTimestampJavaType.INSTANCE.unwrap( toTimestamp( value ), type, options );
			case DATE -> JdbcDateJavaType.INSTANCE.unwrap( toDate( value ), type, options );
			case TIME -> JdbcTimeJavaType.INSTANCE.unwrap( toTime( value ), type, options );
		};
	}

	@Override
	public <X> Date wrap(X value, WrapperOptions options) {
		return switch ( precision ) {
			case TIMESTAMP -> JdbcTimestampJavaType.INSTANCE.wrap( value, options );
			case DATE -> JdbcDateJavaType.INSTANCE.wrap( value, options );
			case TIME -> JdbcTimeJavaType.INSTANCE.wrap( value, options );
		};
	}

	@Override
	public Object coerce(Object value) {
		try {
			return wrap( value, null );
		}
		catch (Exception e) {
			throw CoercionHelper.coercionException( e );
		}
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		return switch ( precision ) {
			case TIMESTAMP -> JdbcTimestampJavaType.INSTANCE.isWider( javaType );
			case DATE -> JdbcDateJavaType.INSTANCE.isWider( javaType );
			case TIME -> JdbcTimeJavaType.INSTANCE.isWider( javaType );
		};
	}

	@Override
	public Date next(
			Date current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return seed( length, precision, scale, session );
	}

	@Override
	public Date seed(
			Long length,
			Integer precision, Integer scale, SharedSessionContractImplementor session) {
		return Timestamp.from( ClockHelper.forPrecision( precision, session ).instant() );
	}

	private static Timestamp toTimestamp(Date date) {
		return date instanceof Timestamp timestamp
				? timestamp
				: JdbcTimestampJavaType.wrapSqlTimestamp( date );
	}

	private static Time toTime(Date date) {
		return date instanceof Time time
				? time
				: JdbcTimeJavaType.toTime( date );
	}

	private static java.sql.Date toDate(java.util.Date value) {
		return JdbcDateJavaType.toDate( value );
	}

}
