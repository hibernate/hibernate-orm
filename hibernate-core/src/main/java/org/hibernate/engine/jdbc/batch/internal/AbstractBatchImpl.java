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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import org.hibernate.Logger;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.spi.SQLExceptionHelper;
import org.hibernate.engine.jdbc.spi.SQLStatementLogger;

/**
 * Convenience base class for implementors of the Batch interface.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractBatchImpl implements Batch {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                AbstractBatchImpl.class.getPackage().getName());

	private final SQLStatementLogger statementLogger;
	private final SQLExceptionHelper exceptionHelper;
	private Object key;
	private LinkedHashMap<String,PreparedStatement> statements = new LinkedHashMap<String,PreparedStatement>();
	private LinkedHashSet<BatchObserver> observers = new LinkedHashSet<BatchObserver>();

	protected AbstractBatchImpl(Object key,
								SQLStatementLogger statementLogger,
								SQLExceptionHelper exceptionHelper) {
		if ( key == null || statementLogger == null || exceptionHelper == null ) {
			throw new IllegalArgumentException( "key, statementLogger, and exceptionHelper must be non-null." );
		}
		this.key = key;
		this.statementLogger = statementLogger;
		this.exceptionHelper = exceptionHelper;
	}

	/**
	 * Perform batch execution.
	 * <p/>
	 * This is called from the explicit {@link #execute() execution}, but may also be called from elsewhere
	 * depending on the exact implementation.
	 */
	protected abstract void doExecuteBatch();

	/**
	 * Convenience access to the SQLException helper.
	 *
	 * @return The underlying SQLException helper.
	 */
	protected SQLExceptionHelper getSqlExceptionHelper() {
		return exceptionHelper;
	}

	/**
	 * Convenience access to the SQL statement logger.
	 *
	 * @return The underlying JDBC services.
	 */
	protected SQLStatementLogger getSqlStatementLogger() {
		return statementLogger;
	}

	/**
	 * Access to the batch's map of statements (keyed by SQL statement string).
	 *
	 * @return This batch's statements.
	 */
	protected LinkedHashMap<String,PreparedStatement> getStatements() {
		return statements;
	}

	/**
	 * {@inheritDoc}
	 */
	public final Object getKey() {
		return key;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addObserver(BatchObserver observer) {
		observers.add( observer );
	}

	/**
	 * {@inheritDoc}
	 */
	public final PreparedStatement getBatchStatement(Object key, String sql) {
		checkConsistentBatchKey( key );
		if ( sql == null ) {
			throw new IllegalArgumentException( "sql must be non-null." );
		}
		PreparedStatement statement = statements.get( sql );
		if ( statement != null ) {
            LOG.debug("Reusing prepared statement");
			statementLogger.logStatement( sql );
		}
		return statement;
	}

	/**
	 * {@inheritDoc}
	 */
	// TODO: should this be final???
	@Override
	public void addBatchStatement(Object key, String sql, PreparedStatement preparedStatement) {
		checkConsistentBatchKey( key );
        if (sql == null) throw new IllegalArgumentException("sql must be non-null.");
        if (statements.put(sql, preparedStatement) != null) LOG.preparedStatementAlreadyInBatch(sql);
	}

	protected void checkConsistentBatchKey(Object key) {
		if ( ! this.key.equals( key ) ) {
			throw new IllegalStateException(
					"specified key ["+ key + "] is different from internal batch key [" + this.key + "]."
			);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final void execute() {
		notifyObserversExplicitExecution();
		if ( statements.isEmpty() ) {
			return;
		}
		try {
			try {
				doExecuteBatch();
			}
			finally {
				release();
			}
		}
		finally {
			statements.clear();
		}
	}

	private void releaseStatements() {
		for ( PreparedStatement statement : getStatements().values() ) {
			try {
				statement.close();
			}
			catch ( SQLException e ) {
                LOG.unableToReleaseBatchStatement();
                LOG.sqlExceptionEscapedProxy(e);
			}
		}
		getStatements().clear();
	}

	private void notifyObserversExplicitExecution() {
		for ( BatchObserver observer : observers ) {
			observer.batchExplicitlyExecuted();
		}
	}

	/**
	 * Convenience method to notify registered observers of an implicit execution of this batch.
	 */
	protected void notifyObserversImplicitExecution() {
		for ( BatchObserver observer : observers ) {
			observer.batchImplicitlyExecuted();
		}
	}

	public void release() {
        if (getStatements() != null && !getStatements().isEmpty()) LOG.batchContainedStatementsOnRelease();
		releaseStatements();
		observers.clear();
	}
}
