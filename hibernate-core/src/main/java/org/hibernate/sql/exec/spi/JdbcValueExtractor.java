/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The low-level contract for extracting (reading) values to JDBC.
 *
 * @apiNote At the JDBC-level we always deal with simple/basic values; never
 * composites, entities, collections, etc
 *
 * @author Steve Ebersole
 *
 * @see JdbcValueBinder
 * @see SqlExpressableType
 */
public interface JdbcValueExtractor<J>  {
	/**
	 * Extract value from result set
	 */
	J extract(ResultSet resultSet, int jdbcParameterPosition, ExecutionContext executionContext) throws SQLException;

	/**
	 * Extract value from CallableStatement
	 */
	J extract(
			CallableStatement statement,
			int jdbcParameterPosition,
			ExecutionContext executionContext) throws SQLException;

	/**
	 * Extract value from CallableStatement, by name
	 */
	J extract(CallableStatement statement, String jdbcParameterName, ExecutionContext executionContext) throws SQLException;
}
