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
public final class JdbcIsolationDelegate implements IsolationDelegate {

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

	@Override
	public <T> T delegateWork(WorkExecutorVisitable<T> work, boolean transacted)
			throws HibernateException {
		final Connection connection;
		try {
			connection = connectionAccess.obtainConnection();
		}
		catch ( SQLException sqle ) {
			throw sqlExceptionHelper.convert( sqle, "Unable to obtain isolated JDBC connection" );
		}

		try {
			final boolean wasAutoCommit;
			try {
				wasAutoCommit = disableAutoCommit( transacted, connection );
			}
			catch (SQLException sqle) {
				throw sqlExceptionHelper.convert( sqle, "Unable to manage autocommit on isolated JDBC connection" );
			}

			try {
				return doWorkAndCommit( work, transacted, connection );
			}
			catch (Exception exception) {
				rollBack( transacted, connection, exception );
				if ( exception instanceof HibernateException he ) {
					throw he;
				}
				else if ( exception instanceof SQLException sqle ) {
					throw sqlExceptionHelper.convert( sqle, "Error performing isolated work" );
				}
				else {
					throw new HibernateException( "Error performing isolated work", exception );
				}
			}
			finally {
				resetAutoCommit( transacted, wasAutoCommit, connection );
			}
		}
		finally {
			releaseConnection( connection );
		}
	}

	private static <T> T doWorkAndCommit(WorkExecutorVisitable<T> work, boolean transacted, Connection connection)
			throws SQLException {
		T result = work.accept( new WorkExecutor<>(), connection );
		if ( transacted ) {
			connection.commit();
		}
		return result;
	}

	private void releaseConnection(Connection connection) {
		try {
			connectionAccess.releaseConnection( connection );
		}
		catch ( Exception exception ) {
			JDBC_MESSAGE_LOGGER.unableToReleaseIsolatedConnection( exception );
		}
	}

	private static void rollBack(boolean transacted, Connection connection, Exception original) {
		try {
			if ( transacted && !connection.isClosed() ) {
				connection.rollback();
			}
		}
		catch ( Exception exception ) {
			JDBC_MESSAGE_LOGGER.unableToRollBackIsolatedConnection( exception );
			original.addSuppressed( exception );
		}
	}

	private static void resetAutoCommit(boolean transacted, boolean wasAutoCommit, Connection connection) {
		if ( transacted && wasAutoCommit ) {
			try {
				connection.setAutoCommit( true );
			}
			catch ( Exception exception ) {
				JDBC_MESSAGE_LOGGER.unableToResetAutoCommit( exception );
			}
		}
	}

	private static boolean disableAutoCommit(boolean transacted, Connection connection)
			throws SQLException {
		if ( transacted ) {
			final boolean wasAutoCommit = connection.getAutoCommit();
			if ( wasAutoCommit ) {
				connection.setAutoCommit( false );
			}
			return wasAutoCommit;
		}
		else {
			return false;
		}
	}

	@Override
	public <T> T delegateCallable(Callable<T> callable, boolean transacted)
			throws HibernateException {
		// No connection, nothing to be suspended
		try {
			return callable.call();
		}
		catch ( HibernateException e ) {
			throw e;
		}
		catch ( Exception exception ) {
			throw new HibernateException( exception );
		}
	}
}
