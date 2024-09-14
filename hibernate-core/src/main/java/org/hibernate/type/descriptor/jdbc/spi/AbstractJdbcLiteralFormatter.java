/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
