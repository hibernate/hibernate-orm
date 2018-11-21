/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Optional contract for binding named parameter values to JDBC
 * CallableStatement (procedure).
 *
 * Intended for implementors of either {@link org.hibernate.usertype.UserType} or {@link BasicType}
 *
 * @see BasicType
 * @see org.hibernate.usertype.UserType
 *
 * @author Andrea Boriero
 *
 * @deprecated Ported simply to support {@link BasicType} and
 * {@link org.hibernate.usertype.UserType} as closely as possible to their
 * legacy definitions
 */
@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
public interface ProcedureParameterNamedBinder {
	/**
	 * Bind a value to the JDBC CallableStatement
	 */
	void nullSafeSet(
			CallableStatement statement,
			Object value,
			String name,
			SharedSessionContractImplementor session) throws SQLException;
}
