/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.annotations.Remove;
import org.hibernate.sql.exec.SqlExecLogger;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * The low-level contract for binding (writing) values to JDBC.
 *
 * @apiNote At the JDBC-level we always deal with simple/basic values; never
 * composites, entities, collections, etc
 *
 * @author Steve Ebersole
 *
 * @see JdbcValueExtractor
 */
public interface JdbcValueBinder<J> {
	/**
	 * @deprecated Use {@link SqlExecLogger} instead
	 */
	@Remove
	@Deprecated
	SqlExecLogger BINDING_LOGGER = SqlExecLogger.INSTANCE;

	/**
	 * Bind a value to a prepared statement.
	 */
	void bind(PreparedStatement statement, int parameterPosition, J value, ExecutionContext executionContext) throws SQLException;

	/**
	 * Bind a value to a CallableStatement.
	 *
	 * @apiNote Binding to a CallableStatement by position is done via {@link #bind(PreparedStatement, int, Object, ExecutionContext)} -
	 * CallableStatement extends PreparedStatement
	 */
	void bind(CallableStatement statement, String parameterName, J value, ExecutionContext executionContext) throws SQLException;
}
