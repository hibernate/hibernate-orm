/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;

/**
 * @author Steve Ebersole
 */
public class JavaObjectType extends AbstractSingleColumnStandardBasicType<Object> {
	/**
	 * Singleton access
	 */
	public static final JavaObjectType INSTANCE = new JavaObjectType();

	public JavaObjectType() {
		super( ObjectJdbcType.INSTANCE, ObjectJavaType.INSTANCE );
	}

	public JavaObjectType(JdbcType jdbcType, JavaType<Object> javaType) {
		super( jdbcType, javaType );
	}

	@Override
	public String getName() {
		return "JAVA_OBJECT";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
