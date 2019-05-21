/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Duration;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.spi.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DurationJavaDescriptor extends AbstractBasicJavaDescriptor<Duration> {
	/**
	 * Singleton access
	 */
	public static final DurationJavaDescriptor INSTANCE = new DurationJavaDescriptor();

	@SuppressWarnings("unchecked")
	public DurationJavaDescriptor() {
		super( Duration.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(SqlTypeDescriptorIndicators context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.NUMERIC );
	}

	@Override
	public String toString(Duration value) {
		if ( value == null ) {
			return null;
		}
		return String.valueOf( value.getSeconds() ) + String.valueOf( value.toNanos() );
	}

	@Override
	public Duration fromString(String string) {
		if ( string == null ) {
			return null;
		}
		int cutoff = string.length() - 9;
		return Duration.ofSeconds(
				Long.parseLong( string.substring(0, cutoff) ),
				Long.parseLong( string.substring(cutoff) )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(Duration duration, Class<X> type, SharedSessionContractImplementor session) {
		if ( duration == null ) {
			return null;
		}

		if ( Duration.class.isAssignableFrom( type ) ) {
			return (X) duration;
		}

		if ( BigDecimal.class.isAssignableFrom( type ) ) {
			return (X) new BigDecimal( duration.getSeconds() ).movePointRight(9).add( new BigDecimal( duration.getNano() ) );
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
	public <X> Duration wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		if ( Duration.class.isInstance( value ) ) {
			return (Duration) value;
		}

		if ( BigDecimal.class.isInstance( value ) ) {
			BigDecimal[] secondsAndNanos = ((BigDecimal) value).divideAndRemainder( BigDecimal.ONE.movePointRight(9) );
			return Duration.ofSeconds( secondsAndNanos[0].longValueExact(), secondsAndNanos[1].intValueExact() );
		}

		if ( String.class.isInstance( value ) ) {
			return Duration.parse( (String) value );
		}

		throw unknownWrap( value.getClass() );
	}
}
