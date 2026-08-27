/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test.annotation;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;

public class StubJavaType extends AbstractClassJavaType<StubDomainType> {
	public StubJavaType() {
		super( StubDomainType.class );
	}

	@Override
	public <X> X unwrap(StubDomainType value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( value.value );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> StubDomainType wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof String string ) {
			return new StubDomainType( string );
		}
		throw unknownWrap( value.getClass() );
	}
}
