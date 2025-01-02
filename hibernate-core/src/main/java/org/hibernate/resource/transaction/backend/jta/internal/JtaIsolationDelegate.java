/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal;

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
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.IsolationDelegate;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ExceptionHelper;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * An isolation delegate for JTA environments.
 *
 * @author Andrea Boriero
 */
public class JtaIsolationDelegate implements IsolationDelegate {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JtaIsolationDelegate.class );

	private final JdbcConnectionAccess connectionAccess;
	private final BiFunction<SQLException, String, JDBCException> sqlExceptionConverter;
	private final TransactionManager transactionManager;

	public JtaIsolationDelegate(TransactionCoordinatorOwner transactionCoordinatorOwner, TransactionManager transactionManager) {
		this( transactionCoordinatorOwner.getJdbcSessionOwner(), transactionManager );
	}

	public JtaIsolationDelegate(JdbcSessionOwner jdbcSessionOwner, TransactionManager transactionManager) {
		this(
				jdbcSessionOwner.getJdbcConnectionAccess(),
				jdbcSessionOwner.getSqlExceptionHelper(),
				transactionManager
		);
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
			SQLStateConversionDelegate delegate = new SQLStateConversionDelegate(
					() -> {
						throw new AssertionFailure(
								"Unexpected call to ConversionContext.getViolatedConstraintNameExtractor" );
					}
			);
			this.sqlExceptionConverter = (sqlException, message) -> delegate.convert( sqlException, message, null );
		}
	}

	private JdbcConnectionAccess jdbcConnectionAccess() {
		return connectionAccess;
	}

	private BiFunction<SQLException, String, JDBCException> sqlExceptionConverter() {
		return sqlExceptionConverter;
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

	private static <T> T call(final Callable<T> callable)  {
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
		Throwable originalException = null;
		try {
			// First we suspend any current JTA transaction
			final Transaction surroundingTransaction = transactionManager.suspend();
			if ( surroundingTransaction != null ) {
				LOG.debugf( "Surrounding JTA transaction suspended [%s]", surroundingTransaction );
			}

			try {
				return callable.call();
			}
			catch ( Throwable t1 ) {
				originalException = t1;
			}
			finally {
				try {
					if ( surroundingTransaction != null ) {
						transactionManager.resume( surroundingTransaction );
						LOG.debugf( "Surrounding JTA transaction resumed [%s]", surroundingTransaction );
					}
				}
				catch ( Throwable t2 ) {
					// if the actually work had an error use that, otherwise error based on t
					if ( originalException == null ) {
						originalException = new HibernateException( "Unable to resume previously suspended transaction", t2 );
					}
					else {
						originalException.addSuppressed( t2 ); // No extra nesting, directly t2
					}
				}
			}
		}
		catch ( SystemException e ) {
			originalException = new HibernateException( "Unable to suspend current JTA transaction", e );
		}

		ExceptionHelper.doThrow( originalException );
		return null;
	}

	private <T> T doInNewTransaction(HibernateCallable<T> callable, TransactionManager transactionManager) {
		try {
			// start the new isolated transaction
			transactionManager.begin();
			try {
				T result = callable.call();
				// if everything went ok, commit the isolated transaction
				transactionManager.commit();
				return result;
			}
			catch ( Exception e ) {
				try {
					transactionManager.rollback();
				}
				catch ( Exception exception ) {
					LOG.unableToRollbackIsolatedTransaction( e, exception );
				}
				throw new HibernateException( "Could not apply work", e );
			}
		}
		catch ( SystemException | NotSupportedException e ) {
			throw new HibernateException( "Unable to start isolated transaction", e );
		}
	}

	private <T> T doTheWork(WorkExecutorVisitable<T> work) {
		try {
			// obtain our isolated connection
			Connection connection = jdbcConnectionAccess().obtainConnection();
			try {
				// do the actual work
				return work.accept( new WorkExecutor<>(), connection );
			}
			catch ( HibernateException e ) {
				throw e;
			}
			catch ( Exception e ) {
				throw new HibernateException( "Unable to perform isolated work", e );
			}
			finally {
				try {
					// no matter what, release the connection (handle)
					jdbcConnectionAccess().releaseConnection( connection );
				}
				catch ( Throwable throwable ) {
					LOG.unableToReleaseIsolatedConnection( throwable );
				}
			}
		}
		catch (SQLException e) {
			final JDBCException jdbcException = sqlExceptionConverter().apply( e, "unable to obtain isolated JDBC connection" );
			if ( jdbcException == null ) {
				throw new HibernateException( "Unable to obtain isolated JDBC connection", e );
			}
			throw jdbcException;
		}
	}

	// Callable that does not throw Exception; in Java <8 there's no Supplier
	private interface HibernateCallable<T> {
		T call() throws HibernateException;
	}
}
