/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.process.spi;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.select.SqlSelection;

/**
 * A low-level reader for extracting JDBC results.  We always extract "basic" values
 * via this contract; various other contracts may consume those basic vales into compositions
 * like an entity, embeddable or collection.
 *
 * @author Steve Ebersole
 */
public interface SqlSelectionReader<T> {
	/**
	 * Read a value from the underlying JDBC ResultSet
	 *
	 * @param resultSet The JDBC ResultSet from which to extract a value
	 * @param jdbcValuesSourceProcessingState The current JDBC values processing state
	 * @param sqlSelection Description of the JDBC value to be extracted
	 *
	 * @return The extracted value
	 *
	 * @throws SQLException Exceptions from the underlying JDBC objects are simply re-thrown.
	 */
	T read(
			ResultSet resultSet,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			SqlSelection sqlSelection) throws SQLException;


	/**
	 * Extract the value of an INOUT/OUT parameter from the JDBC CallableStatement *by position*
	 *
	 * @param statement The CallableStatement from which to extract the parameter value.
	 * @param jdbcParameterIndex The index of the registered INOUT/OUT parameter
	 * @param session The Session from which this request originates.
	 *
	 * @return
	 *
	 * @throws SQLException Exceptions from the underlying JDBC objects are simply re-thrown.
	 */
	T extractParameterValue(CallableStatement statement, int jdbcParameterIndex, SharedSessionContractImplementor session) throws SQLException;

	/**
	 * Extract the value of an INOUT/OUT parameter from the JDBC CallableStatement *by name*
	 *
	 * @param statement The CallableStatement from which to extract the parameter value.
	 * @param jdbcParameterName The parameter name.
	 * @param session The Session from which this request originates.
	 *
	 * @return The extracted value.
	 *
	 * @throws SQLException Exceptions from the underlying JDBC objects are simply re-thrown.
	 */
	T extractParameterValue(CallableStatement statement, String jdbcParameterName, SharedSessionContractImplementor session) throws SQLException;
}
