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

import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * An implementation of {@link org.hibernate.engine.jdbc.batch.spi.Batch} which does not perform batching.  It simply
 * executes each statement as it is encountered.
 *
 * @author Steve Ebersole
 */
public class NonBatchingBatch extends AbstractBatchImpl {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			NonBatchingBatch.class.getName()
	);

	private JdbcCoordinator jdbcCoordinator;
	
	protected NonBatchingBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
		super( key, jdbcCoordinator );
		this.jdbcCoordinator = jdbcCoordinator;
	}

	@Override
	public void addToBatch() {
		notifyObserversImplicitExecution();
		for ( Map.Entry<String,PreparedStatement> entry : getStatements().entrySet() ) {
			try {
				final PreparedStatement statement = entry.getValue();
				final int rowCount = jdbcCoordinator.getResultSetReturn().executeUpdate( statement );
				getKey().getExpectation().verifyOutcome( rowCount, statement, 0 );
				jdbcCoordinator.getResourceRegistry().release( statement );
				jdbcCoordinator.afterStatementExecution();
			}
			catch ( SQLException e ) {
				abortBatch();
				throw sqlExceptionHelper().convert( e, "could not execute non-batched batch statement", entry.getKey() );
			}
			catch (JDBCException e) {
				abortBatch();
				throw e;
			}
		}

		getStatements().clear();
	}

	@Override
	protected void clearBatch(PreparedStatement statement) {
		// no need to call PreparedStatement#clearBatch here...
	}

	@Override
	protected void doExecuteBatch() {
		// nothing to do
	}
}
