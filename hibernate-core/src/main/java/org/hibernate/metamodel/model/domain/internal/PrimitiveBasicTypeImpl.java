/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

public class PrimitiveBasicTypeImpl<J> extends BasicTypeImpl<J> {
	private final Class<J> primitiveClass;

	public PrimitiveBasicTypeImpl(JavaType<J> javaType, JdbcType jdbcType, Class<J> primitiveClass) {
		super( javaType, jdbcType );
		assert primitiveClass.isPrimitive();
		this.primitiveClass = primitiveClass;
	}

	@Override
	public Class<J> getJavaType() {
		return primitiveClass;
	}
}
