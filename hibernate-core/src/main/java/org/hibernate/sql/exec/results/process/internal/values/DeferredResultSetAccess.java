/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal.values;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.results.process.internal.values.JdbcValuesSourceResultSetImpl.ResultSetAccess;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.PreparedStatementCreator;
import org.hibernate.sql.exec.spi.PreparedStatementExecutor;

/**
 * @author Steve Ebersole
 */
public class DeferredResultSetAccess implements ResultSetAccess {
	private final SharedSessionContractImplementor persistenceContext;
	private final JdbcSelect jdbcSelect;
	private final QueryOptions queryOptions;
	private final PreparedStatementCreator statementCreator;
	private final PreparedStatementExecutor preparedStatementExecutor;
	private final QueryParameterBindings queryParameterBindings;

	private PreparedStatement preparedStatement;
	private ResultSet resultSet;

	public DeferredResultSetAccess(
			SharedSessionContractImplementor persistenceContext,
			JdbcSelect jdbcSelect,
			QueryOptions queryOptions,
			PreparedStatementCreator statementCreator,
			PreparedStatementExecutor preparedStatementExecutor,
			QueryParameterBindings queryParameterBindings) {
		this.persistenceContext = persistenceContext;
		this.jdbcSelect = jdbcSelect;
		this.queryOptions = queryOptions;
		this.statementCreator = statementCreator;
		this.preparedStatementExecutor = preparedStatementExecutor;
		this.queryParameterBindings = queryParameterBindings;
	}

	@Override
	public ResultSet getResultSet() {
		if ( resultSet == null ) {
			executeQuery();
		}
		return resultSet;
	}

	private void executeQuery() {
		final LogicalConnectionImplementor logicalConnection = persistenceContext.getJdbcCoordinator().getLogicalConnection();
		final Connection connection = logicalConnection.getPhysicalConnection();

		final JdbcServices jdbcServices = persistenceContext.getFactory().getServiceRegistry().getService( JdbcServices.class );

		final String sql = jdbcSelect.getSql();
		try {
			jdbcServices.getSqlStatementLogger().logStatement( sql );

			// prepare the query
			preparedStatement = statementCreator.create( connection, sql );
			logicalConnection.getResourceRegistry().register( preparedStatement, true );

			// set options
			if ( queryOptions.getFetchSize() != null ) {
				preparedStatement.setFetchSize( queryOptions.getFetchSize() );
			}
			if ( queryOptions.getTimeout() != null ) {
				preparedStatement.setQueryTimeout( queryOptions.getTimeout() );
			}

			// todo : limit/offset


			// bind parameters
			// 		todo : validate that all query parameters were bound?
			int paramBindingPosition = 1;
			for ( JdbcParameterBinder parameterBinder : jdbcSelect.getParameterBinders() ) {
				paramBindingPosition += parameterBinder.bindParameterValue(
						preparedStatement,
						paramBindingPosition,
						queryParameterBindings,
						persistenceContext
				);
			}

			resultSet = preparedStatementExecutor.execute( preparedStatement, queryOptions, persistenceContext );
			logicalConnection.getResourceRegistry().register( resultSet, preparedStatement );

		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"JDBC exception executing SQL [" + sql + "]"
			);
		}
		finally {
			logicalConnection.afterStatement();
		}
	}

	@Override
	public void release() {
		if ( resultSet != null ) {
			persistenceContext.getJdbcCoordinator()
					.getLogicalConnection()
					.getResourceRegistry()
					.release( resultSet, preparedStatement );
			resultSet = null;
		}

		if ( preparedStatement != null ) {
			persistenceContext.getJdbcCoordinator()
					.getLogicalConnection()
					.getResourceRegistry()
					.release( preparedStatement );
			preparedStatement = null;
		}
	}
}
