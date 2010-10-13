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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.jdbc.internal.proxy.ProxyBuilder;

/**
 * Convenience base class for implementors of the Batch interface.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractBatchImpl implements Batch {
	private static final Logger log = LoggerFactory.getLogger( AbstractBatchImpl.class );

	private Object key;
	private LogicalConnectionImplementor logicalConnection;
	private Connection connectionProxy;
	private LinkedHashMap<String,PreparedStatement> statements = new LinkedHashMap<String,PreparedStatement>();
	private LinkedHashSet<BatchObserver> observers = new LinkedHashSet<BatchObserver>();

	protected AbstractBatchImpl(Object key, LogicalConnectionImplementor logicalConnection) {
		this.key = key;
		this.logicalConnection = logicalConnection;
		this.connectionProxy = ProxyBuilder.buildConnection( logicalConnection );
	}

	/**
	 * Perform batch execution.
	 * <p/>
	 * This is called from the explicit {@link #execute() execution}, but may also be called from elsewhere
	 * depending on the exact implementation.
	 */
	protected abstract void doExecuteBatch();

	/**
	 * Convenience access to the underlying JDBC services.
	 *
	 * @return The underlying JDBC services.
	 */
	protected JdbcServices getJdbcServices() {
		return logicalConnection.getJdbcServices();
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
	public final PreparedStatement getBatchStatement(String sql, boolean callable) {
		PreparedStatement statement = statements.get( sql );
		if ( statement == null ) {
			statement = buildBatchStatement( sql, callable );
			statements.put( sql, statement );
		}
		else {
			log.debug( "reusing batch statement" );
			getJdbcServices().getSqlStatementLogger().logStatement( sql );
		}
		return statement;
	}

	private PreparedStatement buildBatchStatement(String sql, boolean callable) {
		try {
			if ( callable ) {
				return connectionProxy.prepareCall( sql );
			}
			else {
				return connectionProxy.prepareStatement( sql );
			}
		}
		catch ( SQLException sqle ) {
			log.error( "sqlexception escaped proxy", sqle );
			throw getJdbcServices().getSqlExceptionHelper().convert( sqle, "could not prepare batch statement", sql );
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
				releaseStatements();
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
				log.error( "unable to release batch statement..." );
				log.error( "sqlexception escaped proxy", e );
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
		if ( getStatements() != null && !getStatements().isEmpty() ) {
			log.info( "On release of batch it still contained JDBC statements" );
		}
		releaseStatements();
		observers.clear();
	}
}
