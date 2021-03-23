/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

/**
 * JdbcLiteralFormatter implementation for handling binary literals
 *
 * @author Gavin King
 */
public class JdbcLiteralFormatterBinary extends BasicJdbcLiteralFormatter {
	public JdbcLiteralFormatterBinary(JavaTypeDescriptor javaTypeDescriptor) {
		super( javaTypeDescriptor );
	}

	@Override
	public String toJdbcLiteral(Object value, Dialect dialect, WrapperOptions wrapperOptions) {
		return dialect.formatBinaryLiteral( unwrap( value, byte[].class, wrapperOptions ) );
	}
}
