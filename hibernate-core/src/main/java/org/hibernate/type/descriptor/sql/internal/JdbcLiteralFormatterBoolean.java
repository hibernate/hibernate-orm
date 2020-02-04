/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.BasicJdbcLiteralFormatter;

/**
 * JdbcLiteralFormatter implementation for handling boolean literals
 *
 * @author Steve Ebersole
 */
public class JdbcLiteralFormatterBoolean extends BasicJdbcLiteralFormatter {
	public JdbcLiteralFormatterBoolean(JavaTypeDescriptor javaTypeDescriptor) {
		super( javaTypeDescriptor );
	}

	@Override
	public String toJdbcLiteral(Object value, Dialect dialect, SharedSessionContractImplementor session) {
		return dialect.toBooleanValueString( unwrap( value, Boolean.class, session ) );
	}
}
