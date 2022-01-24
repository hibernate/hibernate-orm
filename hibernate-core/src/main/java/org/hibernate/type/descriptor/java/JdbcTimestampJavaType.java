/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Timestamp} handling.
 *
 * @implSpec Unlike most {@link JavaType} implementations, can handle 2 different "domain
 * representations" (most map just a single type): general {@link Date} values in addition
 * to {@link Timestamp} values.  This capability is shared with
 * {@link JdbcDateJavaType} and {@link JdbcTimeJavaType}.
 */
public class JdbcTimestampJavaType extends AbstractTemporalJavaType<Date> implements VersionJavaType<Date> {
	public static final JdbcTimestampJavaType INSTANCE = new JdbcTimestampJavaType();

	public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

	/**
	 * Intended for use in reading HQL literals and writing SQL literals
	 *
	 * @see #TIMESTAMP_FORMAT
	 */
	@SuppressWarnings("unused")
	public static final DateTimeFormatter LITERAL_FORMATTER = DateTimeFormatter.ofPattern( TIMESTAMP_FORMAT )
			.withZone( ZoneId.from( ZoneOffset.UTC ) );

	public JdbcTimestampJavaType() {
		super( Timestamp.class, TimestampMutabilityPlan.INSTANCE );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	public boolean isInstance(Object value) {
		// this check holds true for java.sql.Timestamp as well
		return value instanceof Date;
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null) {
			return false;
		}

		long t1 = one.getTime();
		long t2 = another.getTime();

		boolean oneIsTimestamp = one instanceof Timestamp;
		boolean anotherIsTimestamp = another instanceof Timestamp;

		int n1 = oneIsTimestamp ? ( (Timestamp) one ).getNanos() : 0;
		int n2 = anotherIsTimestamp ? ( (Timestamp) another ).getNanos() : 0;

		if ( t1 != t2 ) {
			return false;
		}

		if ( oneIsTimestamp && anotherIsTimestamp ) {
			// both are Timestamps
			int nn1 = n1 % 1000000;
			int nn2 = n2 % 1000000;
			return nn1 == nn2;
		}
		else {
			// at least one is a plain old Date
			return true;
		}
	}

	@Override
	public int extractHashCode(Date value) {
		return Long.valueOf( value.getTime() / 1000 ).hashCode();
	}

	@Override
	public Date coerce(Object value, CoercionContext coercionContext) {
		return wrap( value, null );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object unwrap(Date value, Class type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			return value instanceof Timestamp
					? (Timestamp) value
					: new Timestamp( value.getTime() );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			return value;
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			final GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis( value.getTime() );
			return cal;
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return value.getTime();
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return value instanceof java.sql.Date
					? ( java.sql.Date ) value
					: new java.sql.Date( value.getTime() );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return value instanceof java.sql.Time
					? ( java.sql.Time ) value
					: new java.sql.Time( value.getTime() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Date wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Timestamp ) {
			return (Timestamp) value;
		}

		if ( value instanceof Date ) {
			return new Timestamp( ( (Date) value ).getTime() );
		}

		if ( value instanceof Long ) {
			return new Timestamp( (Long) value );
		}

		if ( value instanceof Calendar ) {
			return new Timestamp( ( (Calendar) value ).getTimeInMillis() );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public String toString(Date value) {
		return LITERAL_FORMATTER.format( value.toInstant() );
	}

	@Override
	public Date fromString(CharSequence string) {
		try {
			final TemporalAccessor accessor = LITERAL_FORMATTER.parse( string );
			return new Timestamp(
					accessor.getLong( ChronoField.INSTANT_SECONDS ) * 1000L
							+ accessor.get( ChronoField.NANO_OF_SECOND ) / 1_000_000
			);
		}
		catch ( DateTimeParseException pe) {
			throw new HibernateException( "could not parse timestamp string" + string, pe );
		}
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( Types.TIMESTAMP );
	}

	@Override
	protected <X> TemporalJavaType<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (TemporalJavaType<X>) this;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return dialect.getDefaultTimestampPrecision();
	}

	@Override
	public Date next(Date current, SharedSessionContractImplementor session) {
		return seed( session );
	}

	@Override
	public Date seed(SharedSessionContractImplementor session) {
		return new Timestamp( System.currentTimeMillis() );
	}


	public static class TimestampMutabilityPlan extends MutableMutabilityPlan<Date> {
		public static final TimestampMutabilityPlan INSTANCE = new TimestampMutabilityPlan();
		@Override
		public Date deepCopyNotNull(Date value) {
			if ( value instanceof Timestamp ) {
				// make sure to get the nanos
				final Timestamp orig = (Timestamp) value;
				final Timestamp copy = new Timestamp( orig.getTime() );
				copy.setNanos( orig.getNanos() );
				return copy;
			}
			else {
				return new Timestamp( value.getTime() );
			}
		}
	}
}
