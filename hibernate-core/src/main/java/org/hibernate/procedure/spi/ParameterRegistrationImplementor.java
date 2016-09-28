/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.spi;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.procedure.ParameterRegistration;

/**
 * Additional internal contract for ParameterRegistration
 *
 * @author Steve Ebersole
 */
public interface ParameterRegistrationImplementor<T> extends ParameterRegistration<T> {
	ProcedureCallImplementor getProcedureCall();

	/**
	 * Prepare for execution.
	 *
	 * @param statement The statement about to be executed
	 * @param i The parameter index for this registration (used for positional)
	 *
	 * @throws SQLException Indicates a problem accessing the statement object
	 */
	void prepare(CallableStatement statement, int i) throws SQLException;

	/**
	 * Access to the SQL type(s) for this parameter
	 *
	 * @return The SQL types (JDBC type codes)
	 */
	int[] getSqlTypes();

	/**
	 * Extract value from the statement afterQuery execution (used for OUT/INOUT parameters).
	 *
	 * @param statement The callable statement
	 *
	 * @return The extracted value
	 */
	T extract(CallableStatement statement);

}
