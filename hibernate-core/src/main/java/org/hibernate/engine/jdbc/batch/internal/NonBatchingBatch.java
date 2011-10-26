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

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.internal.CoreMessageLogger;

/**
 * An implementation of {@link org.hibernate.engine.jdbc.batch.spi.Batch} which does not perform batching.  It simply
 * executes each statement as it is encountered.
 *
 * @author Steve Ebersole
 */
public class NonBatchingBatch extends AbstractBatchImpl {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, NonBatchingBatch.class.getName() );

	protected NonBatchingBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
		super( key, jdbcCoordinator );
	}

	@Override
	public void addToBatch() {
		notifyObserversImplicitExecution();
		for ( Map.Entry<String,PreparedStatement> entry : getStatements().entrySet() ) {
			try {
				final PreparedStatement statement = entry.getValue();
				final int rowCount = statement.executeUpdate();
				getKey().getExpectation().verifyOutcome( rowCount, statement, 0 );
				try {
					statement.close();
				}
				catch (SQLException e) {
					LOG.debug( "Unable to close non-batched batch statement", e );
				}
			}
			catch ( SQLException e ) {
				LOG.debug( "SQLException escaped proxy", e );
				throw sqlExceptionHelper().convert( e, "could not execute batch statement", entry.getKey() );
			}
		}
		getStatements().clear();
	}

	@Override
	protected void doExecuteBatch() {
		// nothing to do
	}
}
