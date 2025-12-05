/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.TransactionException;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.IsolationDelegate;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

import static org.hibernate.resource.transaction.backend.jta.internal.JtaLogging.JTA_LOGGER;

/**
 * An isolation delegate for JTA environments.
 *
 * @author Andrea Boriero
 */
public final class JtaIsolationDelegate implements IsolationDelegate {

	private final JdbcConnectionAccess connectionAccess;
	private final BiFunction<SQLException, String, JDBCException> sqlExceptionConverter;
	private final TransactionManager transactionManager;

	public JtaIsolationDelegate(TransactionCoordinatorOwner transactionCoordinatorOwner, TransactionManager transactionManager) {
		this( transactionCoordinatorOwner.getJdbcSessionOwner(), transactionManager );
	}

	public JtaIsolationDelegate(JdbcSessionOwner jdbcSessionOwner, TransactionManager transactionManager) {
		this( jdbcSessionOwner.getJdbcConnectionAccess(), jdbcSessionOwner.getSqlExceptionHelper(), transactionManager );
	}

	public JtaIsolationDelegate(
			JdbcConnectionAccess connectionAccess,
			SqlExceptionHelper sqlExceptionConverter,
			TransactionManager transactionManager) {
		this.connectionAccess = connectionAccess;
		this.transactionManager = transactionManager;
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
		return doInSuspendedTransaction(
				() -> transacted
						? doInNewTransaction( () -> doTheWork( work ), transactionManager )
						: doTheWork( work )
		);
	}

	@Override
	public <T> T delegateCallable(final Callable<T> callable, final boolean transacted) throws HibernateException {
		return doInSuspendedTransaction(
				() -> transacted
						? doInNewTransaction( () -> call( callable ), transactionManager )
						: call( callable ));
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

	private <T> T doInSuspendedTransaction(HibernateCallable<T> callable) {
		final Transaction surroundingTransaction;
		try {
			// suspend current JTA transaction, if any
			surroundingTransaction = suspend();
		}
		catch ( SystemException systemException ) {
			throw new TransactionException( "Unable to suspend current JTA transaction", systemException );
		}

		Throwable exception = null;
		try {
			return callable.call();
		}
		catch ( HibernateException he ) {
			exception = he;
			throw he;
		}
		catch ( Throwable throwable ) {
			exception = throwable;
			throw new HibernateException( "Unable to perform isolated work", throwable );
		}
		finally {
			try {
				// resume the JTA transaction we suspended
				resume( surroundingTransaction );
			}
			catch ( Throwable throwable ) {
				// if the actual work had an error, use that; otherwise throw this error
				if ( exception == null ) {
					throw new TransactionException( "Unable to resume suspended transaction", throwable );
				}
				else {
					exception.addSuppressed( throwable );
				}
			}
		}
	}

	private void resume(Transaction surroundingTransaction) throws InvalidTransactionException, SystemException {
		if ( surroundingTransaction != null ) {
			transactionManager.resume( surroundingTransaction );
			JTA_LOGGER.transactionResumed( surroundingTransaction );
		}
	}

	private Transaction suspend() throws SystemException {
		final var surroundingTransaction = transactionManager.suspend();
		if ( surroundingTransaction != null ) {
			JTA_LOGGER.transactionSuspended( surroundingTransaction );
		}
		return surroundingTransaction;
	}

	private <T> T doInNewTransaction(HibernateCallable<T> callable, TransactionManager transactionManager) {
		try {
			// start the new isolated transaction
			transactionManager.begin();
		}
		catch ( SystemException | NotSupportedException exception ) {
			throw new TransactionException( "Unable to start isolated transaction", exception );
		}

		try {
			T result = callable.call();
			// if everything went ok, commit the isolated transaction
			transactionManager.commit();
			return result;
		}
		catch ( Exception exception ) { //TODO: should this be Throwable
			rollBack( transactionManager, exception );
			if ( exception instanceof HibernateException he ) {
				throw he;
			}
			else {
				throw new HibernateException( "Error performing work", exception );
			}
		}
	}

	private static void rollBack(TransactionManager transactionManager, Exception original) {
		try {
			transactionManager.rollback();
		}
		catch ( Exception exception ) {
			JTA_LOGGER.unableToRollBackIsolatedTransaction( original, exception );
			original.addSuppressed( exception );
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

	// Callable that does not throw Exception; in Java <8 there's no Supplier
	private interface HibernateCallable<T> {
		T call() throws HibernateException;
	}
}
