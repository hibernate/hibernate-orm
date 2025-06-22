/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * @author Steve Ebersole
 */
public class ObjectJavaType extends AbstractClassJavaType<Object> {
	/**
	 * Singleton access
	 */
	public static final ObjectJavaType INSTANCE = new ObjectJavaType();

	public ObjectJavaType() {
		super( Object.class );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public boolean isInstance(Object value) {
		return true;
	}

	@Override
	public <X> X unwrap(Object value, Class<X> type, WrapperOptions options) {
		//noinspection unchecked
		return (X) value;
	}

	@Override
	public <X> Object wrap(X value, WrapperOptions options) {
		return value;
	}

}
