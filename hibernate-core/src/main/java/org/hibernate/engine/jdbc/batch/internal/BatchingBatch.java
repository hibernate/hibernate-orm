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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.jdbc.Expectation;

/**
 * A {@link org.hibernate.engine.jdbc.batch.spi.Batch} implementation which does batching based on a given size.  Once the batch size is exceeded, the
 * batch is implicitly executed.
 *
 * @author Steve Ebersole
 */
public class BatchingBatch extends AbstractBatchImpl {
	private static final Logger log = LoggerFactory.getLogger( BatchingBatch.class );

	private final int batchSize;
	private Expectation[] expectations;
	private int batchPosition;

	public BatchingBatch(Object key, LogicalConnectionImplementor logicalConnection, int batchSize) {
		super( key, logicalConnection );
		this.batchSize = batchSize;
		this.expectations = new Expectation[ batchSize ];
	}

	/**
	 * {@inheritDoc}
	 */
	public void addToBatch(Expectation expectation) {
		if ( !expectation.canBeBatched() ) {
			throw new HibernateException( "attempting to batch an operation which cannot be batched" );
		}
		for ( Map.Entry<String,PreparedStatement> entry : getStatements().entrySet() ) {
			try {
				entry.getValue().addBatch();
			}
			catch ( SQLException e ) {
				log.error( "sqlexception escaped proxy", e );
				throw getJdbcServices().getSqlExceptionHelper().convert( e, "could not perform addBatch", entry.getKey() );
			}
		}
		expectations[ batchPosition++ ] = expectation;
		if ( batchPosition == batchSize ) {
			notifyObserversImplicitExecution();
			doExecuteBatch();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doExecuteBatch() {
		if ( batchPosition == 0 ) {
			log.debug( "no batched statements to execute" );
		}
		else {
			if ( log.isDebugEnabled() ) {
				log.debug( "Executing batch size: " + batchPosition );
			}

			try {
				for ( Map.Entry<String,PreparedStatement> entry : getStatements().entrySet() ) {
					try {
						final PreparedStatement statement = entry.getValue();
						checkRowCounts( statement.executeBatch(), statement );
					}
					catch ( SQLException e ) {
						log.error( "sqlexception escaped proxy", e );
						throw getJdbcServices().getSqlExceptionHelper()
								.convert( e, "could not perform addBatch", entry.getKey() );
					}
				}
			}
			catch ( RuntimeException re ) {
				log.error( "Exception executing batch [{}]", re.getMessage() );
				throw re;
			}
			finally {
				batchPosition = 0;
			}

		}
	}

	private void checkRowCounts(int[] rowCounts, PreparedStatement ps) throws SQLException, HibernateException {
		int numberOfRowCounts = rowCounts.length;
		if ( numberOfRowCounts != batchPosition ) {
			log.warn( "JDBC driver did not return the expected number of row counts" );
		}
		for ( int i = 0; i < numberOfRowCounts; i++ ) {
			expectations[i].verifyOutcome( rowCounts[i], ps, i );
		}
	}

}