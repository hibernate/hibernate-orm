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
import org.hibernate.type.Type;

/**
 * Additional internal contract for ParameterRegistration
 *
 * @author Steve Ebersole
 */
public interface ParameterRegistrationImplementor<T> extends ParameterRegistration<T> {
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
	 * Access to the Hibernate type for this parameter registration
	 *
	 * @return The Hibernate Type
	 */
	Type getHibernateType();

	/**
	 * If no value is bound for this parameter registration, is the passing of NULL
	 * to the JDBC CallableStatement for that parameter enabled?  This effectively controls
	 * whether default values for the argument as defined in the database are applied or not.
	 *
	 * @return {@code true} indicates that NULL will be passed to the JDBC driver, effectively disabling
	 * the application of the default argument value defined in the database; {@code false} indicates
	 * that the parameter will simply be ignored, with the assumption that the corresponding argument
	 * defined a default value.
	 */
	boolean isPassNullsEnabled();

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
