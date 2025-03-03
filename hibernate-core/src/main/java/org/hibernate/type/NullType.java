/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullResolvingJdbcType;

/**
 * @author Christian Beikov
 */
public class NullType extends JavaObjectType {
	/**
	 * Singleton access
	 */
	public static final NullType INSTANCE = new NullType();

	public NullType() {
		super( ObjectNullResolvingJdbcType.INSTANCE, ObjectJavaType.INSTANCE );
	}

	public NullType(JdbcType jdbcType, JavaType<Object> javaType) {
		super( jdbcType, javaType );
	}

	@Override
	public String getName() {
		return "null";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return false;
	}
}
