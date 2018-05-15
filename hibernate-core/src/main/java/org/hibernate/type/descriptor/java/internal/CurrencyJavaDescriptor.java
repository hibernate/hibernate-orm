/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Currency;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@link Currency} handling.
 *
 * @author Steve Ebersole
 */
public class CurrencyJavaDescriptor extends AbstractBasicJavaDescriptor<Currency> {
	public static final CurrencyJavaDescriptor INSTANCE = new CurrencyJavaDescriptor();

	public CurrencyJavaDescriptor() {
		super( Currency.class );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return StringJavaDescriptor.INSTANCE.getJdbcRecommendedSqlType( context );
	}

	@Override
	public String toString(Currency value) {
		return value.getCurrencyCode();
	}

	@Override
	public Currency fromString(String string) {
		return Currency.getInstance( string );
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Currency value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.getCurrencyCode();
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Currency wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return Currency.getInstance( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}
}
