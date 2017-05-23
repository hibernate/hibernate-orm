/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.sql.spi;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * A formatter object for creating JDBC literals of a given type.
 * <p/>
 * Generally this is obtained from the {@link SqlTypeDescriptor#getJdbcLiteralFormatter} method
 * and would be specific to that Java and SQL type.
 * <p/>
 * Performs a similar function as the legacy LiteralType, except continuing the paradigm
 * shift from inheritance to composition/delegation.
 *
 * @author Steve Ebersole
 */
public interface JdbcLiteralFormatter<T> {
	String toJdbcLiteral(T value, Dialect dialect, SharedSessionContractImplementor session);
}
