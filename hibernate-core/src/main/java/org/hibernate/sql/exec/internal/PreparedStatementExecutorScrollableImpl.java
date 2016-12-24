/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.PreparedStatementExecutor;

/**
 * PreparedStatement execution for building {@link ScrollableResults}, which:<ol>
 *     <li>calls {@link PreparedStatement#executeQuery()}</li>
 *     <li>uses the obtained ResultSet to build a ScrollableResults and returns that</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class PreparedStatementExecutorScrollableImpl implements PreparedStatementExecutor {
	/**
	 * Singleton access
	 */
	public static final PreparedStatementExecutorScrollableImpl INSTANCE = new PreparedStatementExecutorScrollableImpl();

	private PreparedStatementExecutorScrollableImpl() {
	}

	@Override
	public ResultSet execute(
			PreparedStatement ps,
			QueryOptions queryOptions,
			SharedSessionContractImplementor session) throws SQLException {
		final LogicalConnectionImplementor logicalConnection = session.getJdbcCoordinator().getLogicalConnection();

		// Execute the query
		final ResultSet resultSet = ps.executeQuery();
		logicalConnection.getResourceRegistry().register( resultSet, ps );

		return resultSet;
	}
}
