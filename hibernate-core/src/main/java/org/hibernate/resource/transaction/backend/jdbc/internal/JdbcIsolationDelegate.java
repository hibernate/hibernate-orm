/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jdbc.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.IsolationDelegate;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_MESSAGE_LOGGER;

/**
 * @author Andrea Boriero
 */
public class JdbcIsolationDelegate implements IsolationDelegate {

	private final JdbcConnectionAccess connectionAccess;
	private final SqlExceptionHelper sqlExceptionHelper;

	public JdbcIsolationDelegate(TransactionCoordinatorOwner transactionCoordinatorOwner) {
		this( transactionCoordinatorOwner.getJdbcSessionOwner() );
	}

	public JdbcIsolationDelegate(JdbcSessionOwner jdbcSessionOwner) {
		this( jdbcSessionOwner.getJdbcConnectionAccess(), jdbcSessionOwner.getSqlExceptionHelper() );
	}

	public JdbcIsolationDelegate(JdbcConnectionAccess connectionAccess, SqlExceptionHelper sqlExceptionHelper) {
		this.connectionAccess = connectionAccess;
		this.sqlExceptionHelper = sqlExceptionHelper;
	}

	protected JdbcConnectionAccess jdbcConnectionAccess() {
		return this.connectionAccess;
	}

	protected SqlExceptionHelper sqlExceptionHelper() {
		return this.sqlExceptionHelper;
	}

	@Override
	public <T> T delegateWork(WorkExecutorVisitable<T> work, boolean transacted) throws HibernateException {
		boolean wasAutoCommit = false;
		try {
			final Connection connection = jdbcConnectionAccess().obtainConnection();
			try {
				if ( transacted ) {
					if ( connection.getAutoCommit() ) {
						wasAutoCommit = true;
						connection.setAutoCommit( false );
					}
				}

				T result = work.accept( new WorkExecutor<>(), connection );

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
				catch ( Exception exception ) {
					JDBC_MESSAGE_LOGGER.unableToRollBackIsolatedConnection( exception );
				}

				if ( e instanceof HibernateException ) {
					throw e;
				}
				else if ( e instanceof SQLException sqle ) {
					throw sqlExceptionHelper().convert( sqle, "Error performing isolated work" );
				}
				else {
					throw new HibernateException( "Error performing isolated work", e );
				}
			}
			finally {
				if ( transacted && wasAutoCommit ) {
					try {
						connection.setAutoCommit( true );
					}
					catch ( Exception ignore ) {
						JDBC_MESSAGE_LOGGER.unableToResetAutoCommit();
					}
				}
				try {
					jdbcConnectionAccess().releaseConnection( connection );
				}
				catch ( Exception ignored ) {
					JDBC_MESSAGE_LOGGER.unableToReleaseIsolatedConnection( ignored );
				}
			}
		}
		catch ( SQLException sqle ) {
			throw sqlExceptionHelper().convert( sqle, "Unable to obtain isolated JDBC connection" );
		}
	}

	@Override
	public <T> T delegateCallable(Callable<T> callable, boolean transacted) throws HibernateException {
		// No connection, nothing to be suspended
		try {
			return callable.call();
		}
		catch ( HibernateException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new HibernateException( e );
		}
	}
}
