/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc.batch.internal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.internal.CoreMessageLogger;

/**
 * A {@link org.hibernate.engine.jdbc.batch.spi.Batch} implementation which does bathing based on a given size.  Once
 * the batch size is reached for a statement in the batch, the entire batch is implicitly executed.
 *
 * @author Steve Ebersole
 */
public class BatchingBatch extends AbstractBatchImpl {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, BatchingBatch.class.getName() );

	// IMPL NOTE : Until HHH-5797 is fixed, there will only be 1 statement in a batch

	private final int batchSize;
	private int batchPosition;
	private int statementPosition;

	public BatchingBatch(
			BatchKey key,
			JdbcCoordinator jdbcCoordinator,
			int batchSize) {
		super( key, jdbcCoordinator );
		if ( ! key.getExpectation().canBeBatched() ) {
			throw new HibernateException( "attempting to batch an operation which cannot be batched" );
		}
		this.batchSize = batchSize;
	}

	private String currentStatementSql;
	private PreparedStatement currentStatement;

	@Override
	public PreparedStatement getBatchStatement(String sql, boolean callable) {
		currentStatementSql = sql;
		currentStatement = super.getBatchStatement( sql, callable );
		return currentStatement;
	}

	@Override
	public void addToBatch() {
		try {
			currentStatement.addBatch();
		}
		catch ( SQLException e ) {
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
			}
			statementPosition = 0;
		}
	}

	@Override
	protected void doExecuteBatch() {
		if ( batchPosition == 0 ) {
			LOG.debug( "No batched statements to execute" );
		}
		else {
			LOG.debugf( "Executing batch size: %s", batchPosition );
			performExecution();
		}
	}

	private void performExecution() {
		try {
			for ( Map.Entry<String,PreparedStatement> entry : getStatements().entrySet() ) {
				try {
					final PreparedStatement statement = entry.getValue();
					checkRowCounts( statement.executeBatch(), statement );
				}
				catch ( SQLException e ) {
					LOG.debug( "SQLException escaped proxy", e );
					throw sqlExceptionHelper().convert( e, "could not perform addBatch", entry.getKey() );
				}
			}
		}
		catch ( RuntimeException re ) {
			LOG.unableToExecuteBatch( re.getMessage() );
			throw re;
		}
		finally {
			batchPosition = 0;
		}
	}

	private void checkRowCounts(int[] rowCounts, PreparedStatement ps) throws SQLException, HibernateException {
		int numberOfRowCounts = rowCounts.length;
		if ( numberOfRowCounts != batchPosition ) {
			LOG.unexpectedRowCounts();
		}
		for ( int i = 0; i < numberOfRowCounts; i++ ) {
			getKey().getExpectation().verifyOutcome( rowCounts[i], ps, i );
		}
	}
}