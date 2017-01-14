/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class JdbcLiteralFormatterBoolean extends BasicJdbcLiteralFormatter {
	public JdbcLiteralFormatterBoolean(JavaTypeDescriptor javaTypeDescriptor) {
		super( javaTypeDescriptor );
	}

	@Override
	@SuppressWarnings("unchecked")
	public String toJdbcLiteral(Object value, Dialect dialect, SharedSessionContractImplementor session) {
		return unwrap( value, Boolean.class, session ).toString();
	}
}
