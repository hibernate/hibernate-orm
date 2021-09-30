/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

/**
 * JdbcLiteralFormatter implementation for handling boolean literals
 *
 * @author Steve Ebersole
 */
public class JdbcLiteralFormatterBoolean extends BasicJdbcLiteralFormatter {
	public JdbcLiteralFormatterBoolean(JavaTypeDescriptor<?> javaTypeDescriptor) {
		super( javaTypeDescriptor );
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, Object value, Dialect dialect, WrapperOptions wrapperOptions) {
		dialect.appendBooleanValueString( appender, unwrap( value, Boolean.class, wrapperOptions ) );
	}
}
