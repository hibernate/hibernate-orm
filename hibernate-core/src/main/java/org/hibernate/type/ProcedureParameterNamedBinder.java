/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Optional {@link Type} contract for implementations enabled
 * to set store procedure OUT/INOUT parameters values by name.
 *
 * @author Andrea Boriero
 */
public interface ProcedureParameterNamedBinder<J> {

	/**
	 * Can the given instance of this type actually set the parameter value by name
	 *
	 * @return {@code true} indicates that @{link #nullSafeSet} calls will not fail
	 */
	boolean canDoSetting();

	/**
	 * Bind a value to the JDBC prepared statement, ignoring some columns as dictated by the 'settable' parameter.
	 * Implementors should handle the possibility of null values.
	 * Does not support multi-column type
	 *
	 * @param statement The CallableStatement to which to bind
	 * @param value the object to write
	 * @param name parameter bind name
	 * @param session The originating session
	 *
	 * @throws HibernateException An error from Hibernate
	 * @throws SQLException An error from the JDBC driver
	 */
	void nullSafeSet(CallableStatement statement, J value, String name, SharedSessionContractImplementor session) throws SQLException;
}
