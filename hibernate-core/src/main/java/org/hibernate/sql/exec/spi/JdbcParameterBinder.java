/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.query.spi.QueryParameterBindings;

/**
 * Performs parameter value binding to a JDBC PreparedStatement.
 *
 * @author Steve Ebersole
 * @author John O'Hara
 */
public interface JdbcParameterBinder {
	/**
	 * Controls how unbound values for this IN/INOUT parameter registration will be handled prior to
	 * execution.  There are 2 possible options to handle it:<ul>
	 *     <li>bind the NULL to the parameter</li>
	 *     <li>do not bind the NULL to the parameter</li>
	 * </ul>
	 * <p/>
	 * The reason for the distinction comes from default values defined on the corresponding
	 * database procedure/function argument.  Any time a value (including NULL) is bound to the
	 * argument, its default value will not be used.  So effectively this setting controls
	 * whether the NULL should be interpreted as "pass the NULL" or as "apply the argument default".
	 *
	 * @see ParameterRegistration#enablePassingNulls
	 */
	boolean shouldBindNullValues();

	int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session) throws SQLException;
}
