/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Optional contract for extracting named parameter values from JDBC
 * CallableStatement (procedure).
 *
 * Intended for implementors of either {@link org.hibernate.usertype.UserType} or {@link BasicType}
 *
 * @see BasicType
 * @see org.hibernate.usertype.UserType
 *
 * @author Steve Ebersole
 *
 * @deprecated Ported simply to support {@link BasicType} and
 * {@link org.hibernate.usertype.UserType} as closely as possible to their
 * legacy definitions
 */
@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
public interface ProcedureParameterExtractionAware<T> {
	/**
	 * Extract a value from JDBC CallableStatement by position
	 *
	 * @implNote Implementors should handle possibility of null values.
	 */
	T extract(CallableStatement rs, int parameterPosition, SharedSessionContractImplementor session)
			throws HibernateException, SQLException;

	/**
	 * Extract a value from JDBC CallableStatement by name
	 *
	 * @implNote Implementors should handle possibility of null values.
	 */
	T extract(CallableStatement rs, String parameterName, SharedSessionContractImplementor session)
			throws HibernateException, SQLException;
}
