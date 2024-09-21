/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.spi;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

/**
 * Abstract {@link JdbcLiteralFormatter} implementation managing the {@link JavaType}
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJdbcLiteralFormatter<T> implements JdbcLiteralFormatter<T> {
	private final JavaType<T> javaType;

	public AbstractJdbcLiteralFormatter(JavaType<T> javaType) {
		this.javaType = javaType;
	}

	protected JavaType<T> getJavaType() {
		return javaType;
	}
}
