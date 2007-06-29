//$Id: BatchingBatcher.java 10040 2006-06-22 19:51:43Z steve.ebersole@jboss.com $
package org.hibernate.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.StaleStateException;

/**
 * An implementation of the <tt>Batcher</tt> interface that
 * actually uses batching
 * @author Gavin King
 */
public class BatchingBatcher extends AbstractBatcher {

	private int batchSize;
	private Expectation[] expectations;
	
	public BatchingBatcher(ConnectionManager connectionManager, Interceptor interceptor) {
		super( connectionManager, interceptor );
		expectations = new Expectation[ getFactory().getSettings().getJdbcBatchSize() ];
	}

	public void addToBatch(Expectation expectation) throws SQLException, HibernateException {
		if ( !expectation.canBeBatched() ) {
			throw new HibernateException( "attempting to batch an operation which cannot be batched" );
		}
		PreparedStatement batchUpdate = getStatement();
		batchUpdate.addBatch();
		expectations[ batchSize++ ] = expectation;
		if ( batchSize == getFactory().getSettings().getJdbcBatchSize() ) {
			doExecuteBatch( batchUpdate );
		}
	}

	protected void doExecuteBatch(PreparedStatement ps) throws SQLException, HibernateException {
		if ( batchSize == 0 ) {
			log.debug( "no batched statements to execute" );
		}
		else {
			if ( log.isDebugEnabled() ) {
				log.debug( "Executing batch size: " + batchSize );
			}

			try {
				checkRowCounts( ps.executeBatch(), ps );
			}
			catch (RuntimeException re) {
				log.error( "Exception executing batch: ", re );
				throw re;
			}
			finally {
				batchSize = 0;
			}

		}

	}

	private void checkRowCounts(int[] rowCounts, PreparedStatement ps) throws SQLException, HibernateException {
		int numberOfRowCounts = rowCounts.length;
		if ( numberOfRowCounts != batchSize ) {
			log.warn( "JDBC driver did not return the expected number of row counts" );
		}
		for ( int i = 0; i < numberOfRowCounts; i++ ) {
			expectations[i].verifyOutcome( rowCounts[i], ps, i );
		}
	}

}






