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

import static org.hibernate.type.descriptor.java.JdbcDateJavaType.toDateEpoch;

/**
 * Descriptor for {@link Date} handling.
 *
 * @author Steve Ebersole
 */
public class DateJavaType extends AbstractTemporalJavaType<Date> implements VersionJavaType<Date> {
	public static final DateJavaType INSTANCE = new DateJavaType();
	private final TemporalType precision;

	public static class DateMutabilityPlan extends MutableMutabilityPlan<Date> {
		public static final DateMutabilityPlan INSTANCE = new DateMutabilityPlan( TemporalType.TIMESTAMP );
		private final TemporalType precision;

		public DateMutabilityPlan(TemporalType precision) {
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
					case TIMESTAMP -> wrapSqlTimestamp( value );
					case DATE -> wrapSqlDate( value );
					case TIME -> wrapSqlTime( value );
				};
			}
		}
	}

	public DateJavaType() {
		super( Date.class, DateMutabilityPlan.INSTANCE );
		this.precision = TemporalType.TIMESTAMP;
	}

	private DateJavaType(TemporalType precision) {
		super( Date.class, new DateMutabilityPlan(precision) );
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
	public TemporalType getPrecision() {
		return precision;
	}

	@Override
	public <X> TemporalJavaType<X> resolveTypeForPrecision(TemporalType precision, TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) new DateJavaType( precision );
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

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) JdbcDateJavaType.INSTANCE;
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) JdbcTimestampJavaType.INSTANCE;
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimePrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) JdbcTimeJavaType.INSTANCE;
	}

	@Override
	public String toString(Date value) {
//		return JdbcTimestampJavaType.LITERAL_FORMATTER.format( value.toInstant() );
		return switch ( precision ) {
			case TIMESTAMP -> JdbcTimestampJavaType.INSTANCE.toString( wrapSqlTimestamp( value ) );
			case DATE -> JdbcDateJavaType.INSTANCE.toString( wrapSqlDate( value ) );
			case TIME -> JdbcTimeJavaType.INSTANCE.toString( wrapSqlTime( value ) );
		};
	}

	@Override
	public Date fromString(CharSequence string) {
		return switch ( precision ) {
			case TIMESTAMP -> JdbcTimestampJavaType.INSTANCE.fromString( string );
			case DATE -> JdbcDateJavaType.INSTANCE.fromString( string );
			case TIME -> JdbcTimeJavaType.INSTANCE.fromString( string );
		};
//		try {
//			final var accessor = JdbcTimestampJavaType.LITERAL_FORMATTER.parse( string );
//			return new Date(
//					accessor.getLong( ChronoField.INSTANT_SECONDS ) * 1000L
//							+ accessor.get( ChronoField.NANO_OF_SECOND ) / 1_000_000
//			);
//		}
//		catch ( DateTimeParseException pe) {
//			throw new HibernateException( "could not parse timestamp string" + string, pe );
//		}
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another) {
			return true;
		}
		return one != null && another != null
			&& switch ( precision ) {
				case DATE -> JdbcDateJavaType.INSTANCE.areEqual( wrapSqlDate( one ), wrapSqlDate( another ) );
				case TIME -> JdbcTimeJavaType.INSTANCE.areEqual( wrapSqlTime( one ), wrapSqlTime( another ) );
				case TIMESTAMP ->
						one instanceof Timestamp timestamp && another instanceof Timestamp anotherTimestamp
							? JdbcTimestampJavaType.INSTANCE.areEqual( timestamp, anotherTimestamp )
							: one.getTime() == another.getTime();
			};
	}

	@Override
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
			case TIMESTAMP -> JdbcTimestampJavaType.INSTANCE.unwrap( wrapSqlTimestamp( value ), type, options );
			case DATE -> JdbcDateJavaType.INSTANCE.unwrap( wrapSqlDate( value ), type, options );
			case TIME -> JdbcTimeJavaType.INSTANCE.unwrap( wrapSqlTime( value ), type, options );
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
		return wrap( value, null );
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

	static Timestamp wrapSqlTimestamp(Date date) {
		return date instanceof Timestamp timestamp ? timestamp : new Timestamp( date.getTime() );
	}

	static Time wrapSqlTime(Date date) {
		return date instanceof Time time ? time : new Time( date.getTime() % 86_400_000 );
	}

	static java.sql.Date wrapSqlDate(java.util.Date value) {
		if ( value instanceof java.sql.Date date ) {
			final long millis = date.getTime();
			final long dateEpoch = toDateEpoch( millis );
			return dateEpoch == millis ? date : new java.sql.Date( dateEpoch );
		}
		else {
			return new java.sql.Date( toDateEpoch( value ) );
		}
	}

}
