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
import org.hibernate.HibernateLogger;
import org.hibernate.engine.jdbc.spi.SQLExceptionHelper;
import org.hibernate.engine.jdbc.spi.SQLStatementLogger;
import org.hibernate.jdbc.Expectation;
import org.jboss.logging.Logger;

/**
 * An implementation of {@link org.hibernate.engine.jdbc.batch.spi.Batch} which does not perform batching.  It simply executes each statement as it is
 * encountered.
 *
 * @author Steve Ebersole
 */
public class NonBatchingBatch extends AbstractBatchImpl {

    private static final HibernateLogger LOG = Logger.getMessageLogger(HibernateLogger.class, NonBatchingBatch.class.getName());

	protected NonBatchingBatch(Object key,
							SQLStatementLogger statementLogger,
							SQLExceptionHelper exceptionHelper) {
		super( key, statementLogger, exceptionHelper );
	}

	public void addToBatch(Object key, String sql, Expectation expectation) {
		checkConsistentBatchKey( key );
		if ( sql == null ) {
			throw new IllegalArgumentException( "sql must be non-null." );
		}
		notifyObserversImplicitExecution();
		try {
			final PreparedStatement statement = getStatements().get( sql );
			final int rowCount = statement.executeUpdate();
			expectation.verifyOutcome( rowCount, statement, 0 );
		}
		catch ( SQLException e ) {
            LOG.sqlExceptionEscapedProxy(e);
			throw getSqlExceptionHelper().convert( e, "could not execute batch statement", sql );
		}
	}

	@Override
    protected void doExecuteBatch() {
		// nothing to do
	}
}
