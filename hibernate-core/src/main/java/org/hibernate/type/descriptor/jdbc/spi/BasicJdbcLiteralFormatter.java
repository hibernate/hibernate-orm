/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
	protected <X> X unwrap(Object value, Class<X> unwrapType, WrapperOptions wrapperOptions) {
		assert value != null;

		// for performance reasons, avoid conversions if we can
		if ( unwrapType.isInstance( value ) ) {
			return (X) value;
		}

		if ( !getJavaType().isInstance( value ) ) {
			final T coerce = getJavaType().coerce( value, wrapperOptions.getSession() );
			if ( unwrapType.isInstance( coerce ) ) {
				return (X) coerce;
			}
			return getJavaType().unwrap(
					coerce,
					unwrapType,
					wrapperOptions
			);
		}

		return getJavaType().unwrap( (T) value, unwrapType, wrapperOptions );
	}
}
