/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.spi;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Support for {@link org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter}
 * implementations with a basic implementation of an {@link #unwrap} method
 *
 * @author Steve Ebersole
 */
public abstract class BasicJdbcLiteralFormatter<T> extends AbstractJdbcLiteralFormatter<T> {
	public BasicJdbcLiteralFormatter(JavaType<T> javaType) {
		super( javaType );
	}

	protected <X> X unwrap(Object value, Class<X> unwrapType, WrapperOptions options) {
		assert value != null;

		// for performance reasons, avoid conversions if we can
		if ( unwrapType.isInstance( value ) ) {
			return unwrapType.cast( value );
		}
		else {
			final var javaType = getJavaType();
			if ( javaType.isInstance( value ) ) {
				final T castValue = javaType.cast( value );
				return javaType.unwrap( castValue, unwrapType, options );
			}
			else {
				final T coerced = javaType.cast( javaType.coerce( value ) );
				return unwrapType.isInstance( coerced )
						? unwrapType.cast( coerced )
						: javaType.unwrap( coerced, unwrapType, options );
			}
		}
	}
}
