/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/// Java descriptor supplied by the external provider fixture.
///
/// @author Steve Ebersole
public final class ExampleJavaType extends AbstractClassJavaType<ExampleTypeValue> {
	public static final ExampleJavaType INSTANCE = new ExampleJavaType();

	private ExampleJavaType() {
		super( ExampleTypeValue.class, ExampleMutabilityPlan.INSTANCE );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return ExampleJdbcType.INSTANCE;
	}

	@Override
	public ExampleTypeValue fromString(CharSequence string) {
		return string == null ? null : new ExampleTypeValue( string.toString() );
	}

	@Override
	public <X> X unwrap(ExampleTypeValue value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( type.isInstance( value ) ) {
			return type.cast( value );
		}
		if ( type == String.class ) {
			return type.cast( value.text() );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> ExampleTypeValue wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof ExampleTypeValue exampleTypeValue ) {
			return exampleTypeValue;
		}
		if ( value instanceof String string ) {
			return new ExampleTypeValue( string );
		}
		throw unknownWrap( value.getClass() );
	}
}
