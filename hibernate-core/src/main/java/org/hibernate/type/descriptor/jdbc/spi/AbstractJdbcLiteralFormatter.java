/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc.spi;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

/**
 * Abstract JdbcLiteralFormatter implementation managing the JavaTypeDescriptor
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJdbcLiteralFormatter implements JdbcLiteralFormatter {
	private final JavaType javaTypeDescriptor;

	public AbstractJdbcLiteralFormatter(JavaType javaTypeDescriptor) {
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	protected JavaType getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}
}
