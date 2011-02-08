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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.HibernateLogger;
import org.hibernate.engine.jdbc.spi.SQLExceptionHelper;
import org.hibernate.engine.jdbc.spi.SQLStatementLogger;
import org.hibernate.jdbc.Expectation;
import org.jboss.logging.Logger;

/**
 * A {@link org.hibernate.engine.jdbc.batch.spi.Batch} implementation which does
 * batching based on a given size.  Once the batch size is reached for a statement
 * in the batch, the entire batch is implicitly executed.
 *
 * @author Steve Ebersole
 */
public class BatchingBatch extends AbstractBatchImpl {

    private static final HibernateLogger LOG = Logger.getMessageLogger(HibernateLogger.class, BatchingBatch.class.getName());

	private final int batchSize;

	// TODO: A Map is used for expectations so it is possible to track when a batch
	// is full (i.e., when the batch for a particular statement exceeds batchSize)
	// Until HHH-5797 is fixed, there will only be 1 statement in a batch, so it won't
	// be necessary to track expectations by statement.
	private Map<String, List<Expectation>>  expectationsBySql;
	private int maxBatchPosition;

	public BatchingBatch(Object key,
						 SQLStatementLogger statementLogger,
						 SQLExceptionHelper exceptionHelper,
						 int batchSize) {
		super( key, statementLogger, exceptionHelper );
		this.batchSize = batchSize;
		this.expectationsBySql = new HashMap<String, List<Expectation>>();
	}

	/**
	 * {@inheritDoc}
	 */
	public void addToBatch(Object key, String sql, Expectation expectation) {
		checkConsistentBatchKey( key );
		if ( sql == null || expectation == null ) {
			throw new AssertionFailure( "sql or expection was null." );
		}
		if ( ! expectation.canBeBatched() ) {
			throw new HibernateException( "attempting to batch an operation which cannot be batched" );
		}
		final PreparedStatement statement = getStatements().get( sql );
		try {
			statement.addBatch();
		}
		catch ( SQLException e ) {
            LOG.sqlExceptionEscapedProxy(e);
			throw getSqlExceptionHelper().convert( e, "could not perform addBatch", sql );
		}
		List<Expectation> expectations = expectationsBySql.get( sql );
		if ( expectations == null ) {
			expectations = new ArrayList<Expectation>( batchSize );
			expectationsBySql.put( sql, expectations );
		}
		expectations.add( expectation );
		maxBatchPosition = Math.max( maxBatchPosition, expectations.size() );

		// TODO: When HHH-5797 is fixed the following if-block should probably be moved before
		// adding the batch to the current statement (to detect that we have finished
		// with the previous entity).
		if ( maxBatchPosition == batchSize ) {
			notifyObserversImplicitExecution();
			doExecuteBatch();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    protected void doExecuteBatch() {
        if (maxBatchPosition == 0) LOG.debugf("No batched statements to execute");
		else {
            LOG.debugf("Executing %s statements with maximum batch size %s", getStatements().size(), maxBatchPosition);

			try {
				executeStatements();
			}
			catch ( RuntimeException re ) {
                LOG.unableToExecuteBatch(re.getMessage());
				throw re;
			}
			finally {
				for ( List<Expectation> expectations : expectationsBySql.values() ) {
					expectations.clear();
				}
				maxBatchPosition = 0;
			}
		}
	}

	private void executeStatements() {
		for ( Map.Entry<String,PreparedStatement> entry : getStatements().entrySet() ) {
			final String sql = entry.getKey();
			final PreparedStatement statement = entry.getValue();
			final List<Expectation> expectations = expectationsBySql.get( sql );
			if ( batchSize < expectations.size() ) {
				throw new IllegalStateException(
						"Number of expectations [" + expectations.size() +
								"] is greater than batch size [" + batchSize +
								"] for statement [" + sql +
								"]"
				);
			}
			if ( expectations.size() > 0 ) {
                LOG.debugf("Executing with batch of size %s: %s", expectations.size(), sql);
				executeStatement( sql, statement, expectations );
				expectations.clear();
            } else LOG.debugf("Skipped executing because batch size is 0: %s", sql);
		}
	}

	private void executeStatement(String sql, PreparedStatement ps, List<Expectation> expectations) {
		try {
			checkRowCounts( sql, ps.executeBatch(), ps, expectations );
		}
		catch ( SQLException e ) {
            LOG.sqlExceptionEscapedProxy(e);
			throw getSqlExceptionHelper()
					.convert( e, "could not execute statement: " + sql );
		}
	}

	private void checkRowCounts(String sql, int[] rowCounts, PreparedStatement ps, List<Expectation> expectations) {
		int numberOfRowCounts = rowCounts.length;
        if (numberOfRowCounts != expectations.size()) LOG.unexpectedRowCounts();
		try {
			for ( int i = 0; i < numberOfRowCounts; i++ ) {
				expectations.get( i ).verifyOutcome( rowCounts[i], ps, i );
			}
		}
		catch ( SQLException e ) {
            LOG.sqlExceptionEscapedProxy(e);
			throw getSqlExceptionHelper()
					.convert( e, "row count verification failed for statement: ", sql );
		}
	}

	@Override
    public void release() {
		expectationsBySql.clear();
		maxBatchPosition = 0;
	}
}