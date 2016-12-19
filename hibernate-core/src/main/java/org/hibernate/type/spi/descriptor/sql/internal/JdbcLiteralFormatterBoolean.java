/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * @author Steve Ebersole
 */
public class JdbcLiteralFormatterBoolean extends BasicJdbcLiteralFormatter {
	public JdbcLiteralFormatterBoolean(JavaTypeDescriptor javaTypeDescriptor) {
		super( javaTypeDescriptor );
	}

	@Override
	public String toJdbcLiteral(Object value, Dialect dialect) {
		return unwrap( value, Boolean.class ) == TRUE ? TRUE.toString() : FALSE.toString();
	}
}
