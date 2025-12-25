/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.IsolationDelegate;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.resource.transaction.spi.TransactionObserver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

import static org.hibernate.resource.transaction.backend.jta.internal.JtaLogging.JTA_LOGGER;

/**
 * An isolation delegate for JTA environments.
 *
 * @author Andrea Boriero
 */
public final class JtaCompensatingIsolationDelegate implements IsolationDelegate {

	private final JdbcConnectionAccess connectionAccess;
	private final BiFunction<SQLException, String, JDBCException> sqlExceptionConverter;
	private final TransactionCoordinator transactionCoordinator;

	public JtaCompensatingIsolationDelegate(
			TransactionCoordinatorOwner transactionCoordinatorOwner,
			TransactionCoordinator transactionCoordinator) {
		this( transactionCoordinatorOwner.getJdbcSessionOwner(),
				transactionCoordinator );
	}

	public JtaCompensatingIsolationDelegate(
			JdbcSessionOwner jdbcSessionOwner,
			TransactionCoordinator transactionCoordinator) {
		this( jdbcSessionOwner.getJdbcConnectionAccess(),
				jdbcSessionOwner.getSqlExceptionHelper(),
				transactionCoordinator );
	}

	public JtaCompensatingIsolationDelegate(
			JdbcConnectionAccess connectionAccess,
			SqlExceptionHelper sqlExceptionConverter,
			TransactionCoordinator transactionCoordinator) {
		this.connectionAccess = connectionAccess;
		this.transactionCoordinator = transactionCoordinator;
		if ( sqlExceptionConverter != null ) {
			this.sqlExceptionConverter = sqlExceptionConverter::convert;
		}
		else {
			var delegate =
					new SQLStateConversionDelegate( () -> {
						throw new AssertionFailure(
								"Unexpected call to ConversionContext.getViolatedConstraintNameExtractor" );
					} );
			this.sqlExceptionConverter = (sqlException, message) -> delegate.convert( sqlException, message, null );
		}
	}

	@Override
	public <T> T delegateWork(final WorkExecutorVisitable<T> work, final boolean transacted) throws HibernateException {
		return doTheWork( work );
	}

	@Override
	public <T> T delegateCallable(final Callable<T> callable, final boolean transacted) throws HibernateException {
		return call( callable );
	}

	private static <T> T call(final Callable<T> callable) {
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

	private <T> T doTheWork(WorkExecutorVisitable<T> work) {
		final Connection connection;
		try {
			// obtain our isolated connection
			connection = connectionAccess.obtainConnection();
		}
		catch ( SQLException sqle ) {
			throw convert( sqle, "Unable to obtain isolated JDBC connection"  );
		}

		registerCompensatingAction( work );

		try {
			// do the actual work
			return work.accept( new WorkExecutor<>(), connection );
		}
		catch ( HibernateException he ) {
			throw he;
		}
		catch (SQLException sqle) {
			throw convert( sqle, "Error performing isolated work" );
		}
		catch ( Exception e ) {
			throw new HibernateException( "Error performing isolated work", e );
		}
		finally {
			// no matter what, release the connection (handle)
			releaseConnection( connection );
		}
	}

	private <T> void registerCompensatingAction(WorkExecutorVisitable<T> work) {
		// allow the WorkExecutorVisitable to perform some compensating
		// rollback operation if the transaction ultimately fails
		work.begin();
		transactionCoordinator.addObserver( new TransactionObserver() {
			@Override
			public void afterCompletion(boolean successful, boolean delayed) {
				transactionCoordinator.removeObserver( this );
				if ( successful ) {
					work.commit();
				}
				else {
					work.rollback();
				}
			}
		} );
	}

	private void releaseConnection(Connection connection) {
		try {
			connectionAccess.releaseConnection( connection );
		}
		catch ( Throwable throwable ) {
			JTA_LOGGER.unableToReleaseIsolatedConnection( throwable );
		}
	}

	private HibernateException convert(SQLException sqle, String message) {
		final var jdbcException = sqlExceptionConverter.apply( sqle, message );
		return jdbcException == null ? new HibernateException( message, sqle ) : jdbcException;
	}
}
