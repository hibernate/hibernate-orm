/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.usertypes.xmlmapping;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;

public class CurrencyUnitTypeDescriptor extends AbstractClassJavaType<CurrencyUnit> {

	public static final CurrencyUnitTypeDescriptor INSTANCE = new CurrencyUnitTypeDescriptor();

	public CurrencyUnitTypeDescriptor() {
		super( CurrencyUnit.class );
	}

	@Override
	public String toString(CurrencyUnit value) {
		return value.getCurrencyCode();
	}

	@Override
	public CurrencyUnit fromString(CharSequence currencyCode) {
		return new CurrencyUnit() {
			@Override
			public String getCurrencyCode() {
				return currencyCode.toString();
			}

			@Override
			public int getNumericCode() {
				return 0;
			}
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(CurrencyUnit currencyUnit, Class<X> type, WrapperOptions options) { // NOSONAR
		if ( currencyUnit == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) currencyUnit.getCurrencyCode();
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> CurrencyUnit wrap(X currencyCode, WrapperOptions options) {
		if ( currencyCode == null ) {
			return null;
		}
		if ( currencyCode instanceof String ) {
			return new AccountCurrencyUnit( currencyCode.toString(), 0 );
		}
		throw unknownWrap( currencyCode.getClass() );
	}
}
