/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.internal;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.sqm.convert.spi.Return;
import org.hibernate.sql.sqm.exec.results.internal.ResultSetProcessingStateStandardImpl;
import org.hibernate.sql.sqm.exec.results.internal.RowReaderStandardImpl;
import org.hibernate.sql.sqm.exec.results.spi.ResultSetProcessingOptions;
import org.hibernate.sql.sqm.exec.results.spi.ResultSetProcessingState;
import org.hibernate.sql.sqm.exec.results.spi.RowReader;
import org.hibernate.sql.sqm.exec.spi.PreparedStatementExecutor;
import org.hibernate.sql.sqm.exec.spi.QueryOptions;
import org.hibernate.sql.sqm.exec.spi.RowTransformer;

/**
 * Normal PreparedStatement execution which:<ol>
 *     <li>calls {@link PreparedStatement#executeQuery()}</li>
 *     <li>immediately reads all the rows in the ResultSet returning a List of the transformed results</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class PreparedStatementExecutorNormalImpl<T> implements PreparedStatementExecutor<List<T>, T> {
	/**
	 * Singleton access
	 */
	public static final PreparedStatementExecutorNormalImpl INSTANCE = new PreparedStatementExecutorNormalImpl();

	/**
	 * Processing options effectively are only used for entity loading.  Here we don't need these values.
	 */
	private final ResultSetProcessingOptions processingOptions = new ResultSetProcessingOptions() {
		@Override
		public Object getEffectiveOptionalObject() {
			return null;
		}

		@Override
		public String getEffectiveOptionalEntityName() {
			return null;
		}

		@Override
		public Serializable getEffectiveOptionalId() {
			return null;
		}

		@Override
		public boolean shouldReturnProxies() {
			return true;
		}
	};

	@Override
	public List<T> execute(
			PreparedStatement ps,
			QueryOptions queryOptions,
			List<Return> returns,
			RowTransformer<T> rowTransformer,
			SharedSessionContractImplementor session) throws SQLException {
		final LogicalConnectionImplementor logicalConnection = session.getJdbcCoordinator().getLogicalConnection();

		// Execute the query
		final ResultSet resultSet = ps.executeQuery();
		logicalConnection.getResourceRegistry().register( resultSet, ps );

		try {
			int position = 1;

			// Prepare the ResultSetProcessingState...
			final ResultSetProcessingState resultSetProcessingState = new ResultSetProcessingStateStandardImpl(
					resultSet,
					queryOptions,
					returns,
					session
			);

			final RowReader<T> rowReader = new RowReaderStandardImpl<T>( returns, rowTransformer );

			final List<T> results = new ArrayList<T>();
			final Integer maxRows = queryOptions.getLimit().getMaxRows();

			try {
				while ( ( maxRows != null && position <= maxRows ) || resultSet.next() ) {
					results.add(
							rowReader.readRow(
									resultSetProcessingState.getCurrentRowProcessingState(),
									processingOptions
							)
					);

					position++;
					resultSetProcessingState.getCurrentRowProcessingState().finishRowProcessing();
				}

				resultSetProcessingState.finishResultSetProcessing();
			}
			finally {
				resultSetProcessingState.release();
			}

			return results;
		}
		finally {
			logicalConnection.getResourceRegistry().release( resultSet, ps );
			logicalConnection.getResourceRegistry().release( ps );
		}
	}
}
