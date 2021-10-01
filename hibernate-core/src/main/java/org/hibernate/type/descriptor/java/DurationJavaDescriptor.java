/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.math.BigDecimal;
import java.time.Duration;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NumericTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * Descriptor for {@link Duration}, which is represented internally
 * as ({@code long seconds}, {@code int nanoseconds}), approximately
 * 28 decimal digits of precision. This quantity must be stored in
 * the database as a single integer with units of nanoseconds, since
 * the ANSI SQL {@code interval} type is not well-supported.
 *
 * In practice, the 19 decimal digits of a SQL {@code bigint} are
 * capable of representing six centuries in nanoseconds and are
 * sufficient for many applications. However, by default, we map
 * Java {@link Duration} to SQL {@code numeric(21)} here, which
 * can comfortably represent 60 millenia of nanos.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class DurationJavaDescriptor extends AbstractClassTypeDescriptor<Duration> {
	/**
	 * Singleton access
	 */
	public static final DurationJavaDescriptor INSTANCE = new DurationJavaDescriptor();

	@SuppressWarnings("unchecked")
	public DurationJavaDescriptor() {
		super( Duration.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public JdbcTypeDescriptor getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		return NumericTypeDescriptor.INSTANCE;
	}

	@Override
	public String toString(Duration value) {
		if ( value == null ) {
			return null;
		}
		String seconds = String.valueOf( value.getSeconds() );
		String nanos = String.valueOf( value.getNano() );
		String zeros = StringHelper.repeat( '0', 9-nanos.length() );
		return seconds + zeros + nanos;
	}

	@Override
	public Duration fromString(CharSequence string) {
		if ( string == null ) {
			return null;
		}
		int cutoff = string.length() - 9;
		return Duration.ofSeconds(
				Long.parseLong( string.subSequence( 0, cutoff ).toString() ),
				Long.parseLong( string.subSequence( cutoff, string.length() ).toString() )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(Duration duration, Class<X> type, WrapperOptions options) {
		if ( duration == null ) {
			return null;
		}

		if ( Duration.class.isAssignableFrom( type ) ) {
			return (X) duration;
		}

		if ( BigDecimal.class.isAssignableFrom( type ) ) {
			return (X) new BigDecimal( duration.getSeconds() ).movePointRight(9)
					.add( new BigDecimal( duration.getNano() ) );
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return (X) duration.toString();
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( duration.toNanos() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Duration wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( Duration.class.isInstance( value ) ) {
			return (Duration) value;
		}

		if ( BigDecimal.class.isInstance( value ) ) {
			BigDecimal[] secondsAndNanos =
					((BigDecimal) value).divideAndRemainder( BigDecimal.ONE.movePointRight(9) );
			return Duration.ofSeconds(
					secondsAndNanos[0].longValueExact(),
					// use intValue() not intValueExact() here, because
					// the database will sometimes produce garbage digits
					// in a floating point multiplication, and we would
					// get an unwanted ArithmeticException
					secondsAndNanos[1].intValue()
			);
		}

		if ( Long.class.isInstance( value ) ) {
			return Duration.ofNanos( (Long) value );
		}

		if ( String.class.isInstance( value ) ) {
			return Duration.parse( (String) value );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect) {
		// 19+9 = 28 digits is the maximum possible Duration
		// precision, but is an unnecessarily large default,
		// except for cosmological applications. Thirty
		// millenia in both timelike directions should be
		// sufficient time for most businesses!
		return Math.min( 21, dialect.getDefaultDecimalPrecision() );
	}

	@Override
	public int getDefaultSqlScale() {
		return 0;
	}
}
