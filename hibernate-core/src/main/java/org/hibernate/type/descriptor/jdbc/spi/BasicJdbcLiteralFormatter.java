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

	@SuppressWarnings("unchecked")
	protected <X> X unwrap(Object value, Class<X> unwrapType, WrapperOptions options) {
		assert value != null;

		// for performance reasons, avoid conversions if we can
		if ( unwrapType.isInstance( value ) ) {
			return (X) value;
		}
		else {
			final JavaType<T> javaType = getJavaType();
			if ( !javaType.isInstance( value ) ) {
				final T coerced = javaType.coerce( value, options::getTypeConfiguration );
				if ( unwrapType.isInstance( coerced ) ) {
					return (X) coerced;
				}
				else {
					return javaType.unwrap( coerced, unwrapType, options );
				}
			}
			else {
				return javaType.unwrap( (T) value, unwrapType, options );
			}
		}
	}
}
