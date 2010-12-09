/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.SQLExceptionHelper;

/**
 * An implementation of the <tt>Batcher</tt> interface that
 * actually uses batching
 * @author Gavin King
 */
public class BatchingBatcher extends AbstractBatcher {

	private Expectation[] expectations;
	
	private int currentSize;
	public BatchingBatcher(SQLExceptionHelper exceptionHelper, int jdbcBatchSize) {
		super( exceptionHelper, jdbcBatchSize );
		expectations = new Expectation[ jdbcBatchSize ];
		currentSize = 0;
	}

	public void addToBatch(Expectation expectation) throws SQLException, HibernateException {
		if ( !expectation.canBeBatched() ) {
			throw new HibernateException( "attempting to batch an operation which cannot be batched" );
		}
		PreparedStatement batchUpdate = getStatement();
		batchUpdate.addBatch();
		expectations[ currentSize++ ] = expectation;
		if ( currentSize == getJdbcBatchSize() ) {
			doExecuteBatch( batchUpdate );
		}
	}

	protected void doExecuteBatch(PreparedStatement ps) throws SQLException, HibernateException {
		if ( currentSize == 0 ) {
			log.debug( "no batched statements to execute" );
		}
		else {
			if ( log.isDebugEnabled() ) {
				log.debug( "Executing batch size: " + currentSize );
			}

			try {
				checkRowCounts( ps.executeBatch(), ps );
			}
			catch (RuntimeException re) {
				log.error( "Exception executing batch: ", re );
				throw re;
			}
			finally {
				currentSize = 0;
			}

		}
	}

	private void checkRowCounts(int[] rowCounts, PreparedStatement ps) throws SQLException, HibernateException {
		int numberOfRowCounts = rowCounts.length;
		if ( numberOfRowCounts != currentSize ) {
			log.warn( "JDBC driver did not return the expected number of row counts" );
		}
		for ( int i = 0; i < numberOfRowCounts; i++ ) {
			expectations[i].verifyOutcome( rowCounts[i], ps, i );
		}
	}

}






