/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.sqm.exec.spi.PreparedStatementExecutor;
import org.hibernate.sql.sqm.exec.spi.QueryOptions;
import org.hibernate.sql.sqm.exec.spi.RowTransformer;
import org.hibernate.sql.sqm.convert.spi.NotYetImplementedException;
import org.hibernate.sql.sqm.convert.spi.Return;

/**
 * PreparedStatement execution for building {@link ScrollableResults}, which:<ol>
 *     <li>calls {@link PreparedStatement#executeQuery()}</li>
 *     <li>uses the obtained ResultSet to build a ScrollableResults and returns that</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class PreparedStatementExecutorScrollableImpl<T> implements PreparedStatementExecutor<ScrollableResults, T> {
	/**
	 * Singleton access
	 */
	public static final PreparedStatementExecutorScrollableImpl INSTANCE = new PreparedStatementExecutorScrollableImpl();

	@Override
	public ScrollableResults execute(
			PreparedStatement ps,
			QueryOptions queryOptions,
			List<Return> returns,
			RowTransformer<T> rowTransformer,
			SharedSessionContractImplementor session) throws SQLException {
		final LogicalConnectionImplementor logicalConnection = session.getJdbcCoordinator().getLogicalConnection();

		// Execute the query
		final ResultSet resultSet = ps.executeQuery();
		logicalConnection.getResourceRegistry().register( resultSet, ps );

//		new ScrollableResultsImpl(
//				resultSet,
//				rowTransformer,
//				new ScrollableRowReader(
//						returns,
//		...
//		)
//		);
		throw new NotYetImplementedException();
	}
}
