/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.values;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.PreparedStatementCreator;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class DeferredResultSetAccess extends AbstractResultSetAccess {
	private static final Logger log = CoreLogging.logger( DeferredResultSetAccess.class );

	private final JdbcSelect jdbcSelect;
	private final ExecutionContext executionContext;
	private final PreparedStatementCreator statementCreator;

	private PreparedStatement preparedStatement;
	private ResultSet resultSet;

	public DeferredResultSetAccess(
			JdbcSelect jdbcSelect,
			ExecutionContext executionContext,
			PreparedStatementCreator statementCreator) {
		super( executionContext.getSession() );
		this.executionContext = executionContext;
		this.jdbcSelect = jdbcSelect;
		this.statementCreator = statementCreator;
	}

	@Override
	public ResultSet getResultSet() {
		if ( resultSet == null ) {
			executeQuery();
		}
		return resultSet;
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return executionContext.getSession().getFactory();
	}

	private void executeQuery() {
		final LogicalConnectionImplementor logicalConnection = getPersistenceContext().getJdbcCoordinator().getLogicalConnection();
		final Connection connection = logicalConnection.getPhysicalConnection();

		final JdbcServices jdbcServices = getPersistenceContext().getFactory().getServiceRegistry().getService( JdbcServices.class );

		final String sql = jdbcSelect.getSql();

		try {
			log.tracef( "Executing query to retrieve ResultSet : %s", sql );
			jdbcServices.getSqlStatementLogger().logStatement( sql );

			// prepare the query
			preparedStatement = statementCreator.create( connection, sql );
			logicalConnection.getResourceRegistry().register( preparedStatement, true );

			// set options
			if ( executionContext.getQueryOptions().getFetchSize() != null ) {
				preparedStatement.setFetchSize( executionContext.getQueryOptions().getFetchSize() );
			}
			if ( executionContext.getQueryOptions().getTimeout() != null ) {
				preparedStatement.setQueryTimeout( executionContext.getQueryOptions().getTimeout() );
			}

			// todo : limit/offset


			// bind parameters
			// 		todo : validate that all query parameters were bound?
			int paramBindingPosition = 1;
			for ( JdbcParameterBinder parameterBinder : jdbcSelect.getParameterBinders() ) {
				paramBindingPosition += parameterBinder.bindParameterValue(
						preparedStatement,
						paramBindingPosition,
						executionContext
				);
			}

			resultSet = preparedStatement.executeQuery();
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
			getPersistenceContext().getJdbcCoordinator()
					.getLogicalConnection()
					.getResourceRegistry()
					.release( resultSet, preparedStatement );
			resultSet = null;
		}

		if ( preparedStatement != null ) {
			getPersistenceContext().getJdbcCoordinator()
					.getLogicalConnection()
					.getResourceRegistry()
					.release( preparedStatement );
			preparedStatement = null;
		}
	}
}
