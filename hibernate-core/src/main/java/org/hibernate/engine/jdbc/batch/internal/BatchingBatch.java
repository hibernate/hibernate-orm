/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * A {@link org.hibernate.engine.jdbc.batch.spi.Batch} implementation which does bathing based on a given size.  Once
 * the batch size is reached for a statement in the batch, the entire batch is implicitly executed.
 *
 * @author Steve Ebersole
 */
public class BatchingBatch extends AbstractBatchImpl {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			BatchingBatch.class.getName()
	);

	// IMPL NOTE : Until HHH-5797 is fixed, there will only be 1 statement in a batch

	private int batchSize;
	private final int configuredBatchSize;
	private int batchPosition;
	private boolean batchExecuted;
	private int statementPosition;

	/**
	 * Constructs a BatchingBatch
	 *
	 * @param key The batch key
	 * @param jdbcCoordinator The JDBC jdbcCoordinator
	 * @param batchSize The batch size.
	 */
	public BatchingBatch(
			BatchKey key,
			JdbcCoordinator jdbcCoordinator,
			int batchSize) {
		super( key, jdbcCoordinator );
		if ( ! key.getExpectation().canBeBatched() ) {
			throw new HibernateException( "attempting to batch an operation which cannot be batched" );
		}
		this.batchSize = batchSize;
		this.configuredBatchSize = batchSize;
	}

	private String currentStatementSql;
	private PreparedStatement currentStatement;

	@Override
	public PreparedStatement getBatchStatement(String sql, boolean callable) {
		currentStatementSql = sql;
		int previousBatchSize = getStatements().size();
		currentStatement = super.getBatchStatement( sql, callable );
		int currentBatchSize = getStatements().size();
		if ( currentBatchSize > previousBatchSize ) {
			this.batchSize = this.configuredBatchSize * currentBatchSize;
		}
		return currentStatement;
	}

	@Override
	public void addToBatch() {
		try {
			currentStatement.addBatch();
		}
		catch ( SQLException e ) {
			abortBatch();
			LOG.debugf( "SQLException escaped proxy", e );
			throw sqlExceptionHelper().convert( e, "could not perform addBatch", currentStatementSql );
		}
		statementPosition++;
		if ( statementPosition >= getKey().getBatchedStatementCount() ) {
			batchPosition++;
			if ( batchPosition == batchSize ) {
				notifyObserversImplicitExecution();
				performExecution();
				batchPosition = 0;
				batchExecuted = true;
			}
			statementPosition = 0;
		}
	}

	@Override
	protected void doExecuteBatch() {
		if (batchPosition == 0 ) {
			if(! batchExecuted) {
				LOG.debug( "No batched statements to execute" );
			}
		}
		else {
			performExecution();
		}
	}

	private void performExecution() {
		LOG.debugf( "Executing batch size: %s", batchPosition );
		try {
			for ( Map.Entry<String,PreparedStatement> entry : getStatements().entrySet() ) {
				String sql = entry.getKey();
				try {
					final PreparedStatement statement = entry.getValue();
					final int[] rowCounts;
					try {
						getJdbcCoordinator().getJdbcSessionOwner().getJdbcSessionContext().getObserver().jdbcExecuteBatchStart();
						rowCounts = statement.executeBatch();
					}
					finally {
						getJdbcCoordinator().getJdbcSessionOwner().getJdbcSessionContext().getObserver().jdbcExecuteBatchEnd();
					}
					checkRowCounts( rowCounts, statement );
				}
				catch ( SQLException e ) {
					abortBatch();
					LOG.unableToExecuteBatch( e, sql );
					throw sqlExceptionHelper().convert( e, "could not execute batch", sql );
				}
				catch ( RuntimeException re ) {
					abortBatch();
					LOG.unableToExecuteBatch( re, sql );
					throw re;
				}
			}
		}
		finally {
			batchPosition = 0;
		}
	}

	private void checkRowCounts(int[] rowCounts, PreparedStatement ps) throws SQLException, HibernateException {
		final int numberOfRowCounts = rowCounts.length;
		if ( batchPosition != 0 && numberOfRowCounts != batchPosition / getStatements().size() ) {
			LOG.unexpectedRowCounts();
		}
		for ( int i = 0; i < numberOfRowCounts; i++ ) {
			getKey().getExpectation().verifyOutcome( rowCounts[i], ps, i );
		}
	}
}
