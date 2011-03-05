/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.transaction.internal.jdbc;


import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.SQLExceptionHelper;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;

/**
 * The isolation delegate for JDBC {@link Connection} based transactions
 *
 * @author Steve Ebersole
 */
public class JdbcIsolationDelegate implements IsolationDelegate {
	private static final Logger log = LoggerFactory.getLogger( JdbcIsolationDelegate.class );

	private final TransactionCoordinator transactionCoordinator;

	public JdbcIsolationDelegate(TransactionCoordinator transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
	}

	protected ConnectionProvider connectionProvider() {
		return transactionCoordinator.getJdbcCoordinator().getLogicalConnection().getJdbcServices().getConnectionProvider();
	}

	protected SQLExceptionHelper sqlExceptionHelper() {
		return transactionCoordinator.getJdbcCoordinator().getLogicalConnection().getJdbcServices().getSqlExceptionHelper();
	}

	@Override
	public void delegateWork(Work work, boolean transacted) throws HibernateException {
		boolean wasAutoCommit = false;
		try {
			// todo : should we use a connection proxy here?
			Connection connection = connectionProvider().getConnection();
			try {
				if ( transacted ) {
					if ( connection.getAutoCommit() ) {
						wasAutoCommit = true;
						connection.setAutoCommit( false );
					}
				}

				work.execute( connection );

				if ( transacted ) {
					connection.commit();
				}
			}
			catch ( Exception e ) {
				try {
					if ( transacted && !connection.isClosed() ) {
						connection.rollback();
					}
				}
				catch ( Exception ignore ) {
					log.info( "unable to rollback connection on exception [" + ignore + "]" );
				}

				if ( e instanceof HibernateException ) {
					throw (HibernateException) e;
				}
				else if ( e instanceof SQLException ) {
					throw sqlExceptionHelper().convert( (SQLException) e, "error performing isolated work" );
				}
				else {
					throw new HibernateException( "error performing isolated work", e );
				}
			}
			finally {
				if ( transacted && wasAutoCommit ) {
					try {
						connection.setAutoCommit( true );
					}
					catch ( Exception ignore ) {
						log.trace( "was unable to reset connection back to auto-commit" );
					}
				}
				try {
					connectionProvider().closeConnection( connection );
				}
				catch ( Exception ignore ) {
					log.info( "Unable to release isolated connection [" + ignore + "]" );
				}
			}
		}
		catch ( SQLException sqle ) {
			throw sqlExceptionHelper().convert( sqle, "unable to obtain isolated JDBC connection" );
		}
	}

	@Override
	public <T> T delegateWork(ReturningWork<T> work, boolean transacted) throws HibernateException {
		boolean wasAutoCommit = false;
		try {
			// todo : should we use a connection proxy here?
			Connection connection = connectionProvider().getConnection();
			try {
				if ( transacted ) {
					if ( connection.getAutoCommit() ) {
						wasAutoCommit = true;
						connection.setAutoCommit( false );
					}
				}

				T result = work.execute( connection );

				if ( transacted ) {
					connection.commit();
				}

				return result;
			}
			catch ( Exception e ) {
				try {
					if ( transacted && !connection.isClosed() ) {
						connection.rollback();
					}
				}
				catch ( Exception ignore ) {
					log.info( "unable to rollback connection on exception [" + ignore + "]" );
				}

				if ( e instanceof HibernateException ) {
					throw (HibernateException) e;
				}
				else if ( e instanceof SQLException ) {
					throw sqlExceptionHelper().convert( (SQLException) e, "error performing isolated work" );
				}
				else {
					throw new HibernateException( "error performing isolated work", e );
				}
			}
			finally {
				if ( transacted && wasAutoCommit ) {
					try {
						connection.setAutoCommit( true );
					}
					catch ( Exception ignore ) {
						log.trace( "was unable to reset connection back to auto-commit" );
					}
				}
				try {
					connectionProvider().closeConnection( connection );
				}
				catch ( Exception ignore ) {
					log.info( "Unable to release isolated connection [" + ignore + "]" );
				}
			}
		}
		catch ( SQLException sqle ) {
			throw sqlExceptionHelper().convert( sqle, "unable to obtain isolated JDBC connection" );
		}
	}
}
