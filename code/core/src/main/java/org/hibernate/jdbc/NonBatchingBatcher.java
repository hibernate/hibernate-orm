//$Id: NonBatchingBatcher.java 10040 2006-06-22 19:51:43Z steve.ebersole@jboss.com $
package org.hibernate.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;

/**
 * An implementation of the <tt>Batcher</tt> interface that does no batching
 *
 * @author Gavin King
 */
public class NonBatchingBatcher extends AbstractBatcher {

	public NonBatchingBatcher(ConnectionManager connectionManager, Interceptor interceptor) {
		super( connectionManager, interceptor );
	}

	public void addToBatch(Expectation expectation) throws SQLException, HibernateException {
		PreparedStatement statement = getStatement();
		final int rowCount = statement.executeUpdate();
		expectation.verifyOutcome( rowCount, statement, 0 );
	}

	protected void doExecuteBatch(PreparedStatement ps) throws SQLException, HibernateException {
	}

}
