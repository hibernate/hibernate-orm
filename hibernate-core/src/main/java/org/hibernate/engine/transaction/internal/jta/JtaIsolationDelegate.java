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
package org.hibernate.engine.transaction.internal.jta;

import java.sql.Connection;
import java.sql.SQLException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;

/**
 * An isolation delegate for JTA environments.
 *
 * @author Steve Ebersole
 */
public class JtaIsolationDelegate implements IsolationDelegate {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JtaIsolationDelegate.class );

	private final TransactionCoordinator transactionCoordinator;

	public JtaIsolationDelegate(TransactionCoordinator transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
	}

	protected TransactionManager transactionManager() {
		return transactionCoordinator.getTransactionContext()
				.getTransactionEnvironment()
				.getJtaPlatform()
				.retrieveTransactionManager();
	}

	protected JdbcConnectionAccess jdbcConnectionAccess() {
		return transactionCoordinator.getTransactionContext().getJdbcConnectionAccess();
	}

	protected SqlExceptionHelper sqlExceptionHelper() {
		return transactionCoordinator.getTransactionContext()
				.getTransactionEnvironment()
				.getJdbcServices()
				.getSqlExceptionHelper();
	}

	@Override
	public <T> T delegateWork(WorkExecutorVisitable<T> work, boolean transacted) throws HibernateException {
		TransactionManager transactionManager = transactionManager();

		try {
			// First we suspend any current JTA transaction
			Transaction surroundingTransaction = transactionManager.suspend();
			LOG.debugf( "Surrounding JTA transaction suspended [%s]", surroundingTransaction );

			boolean hadProblems = false;
			try {
				// then perform the requested work
				if ( transacted ) {
					return doTheWorkInNewTransaction( work, transactionManager );
				}
				else {
					return doTheWorkInNoTransaction( work );
				}
			}
			catch ( HibernateException e ) {
				hadProblems = true;
				throw e;
			}
			finally {
				try {
					transactionManager.resume( surroundingTransaction );
					LOG.debugf( "Surrounding JTA transaction resumed [%s]", surroundingTransaction );
				}
				catch( Throwable t ) {
					// if the actually work had an error use that, otherwise error based on t
					if ( !hadProblems ) {
						//noinspection ThrowFromFinallyBlock
						throw new HibernateException( "Unable to resume previously suspended transaction", t );
					}
				}
			}
		}
		catch ( SystemException e ) {
			throw new HibernateException( "Unable to suspend current JTA transaction", e );
		}
	}

	private <T> T doTheWorkInNewTransaction(WorkExecutorVisitable<T> work, TransactionManager transactionManager) {
		try {
			// start the new isolated transaction
			transactionManager.begin();

			try {
				T result = doTheWork( work );
				// if everything went ok, commit the isolated transaction
				transactionManager.commit();
				return result;
			}
			catch ( Exception e ) {
				try {
					transactionManager.rollback();
				}
				catch ( Exception ignore ) {
					LOG.unableToRollbackIsolatedTransaction( e, ignore );
				}
				throw new HibernateException( "Could not apply work", e );
			}
		}
		catch ( SystemException e ) {
			throw new HibernateException( "Unable to start isolated transaction", e );
		}
		catch ( NotSupportedException e ) {
			throw new HibernateException( "Unable to start isolated transaction", e );
		}
	}

	private <T> T doTheWorkInNoTransaction(WorkExecutorVisitable<T> work) {
		return doTheWork( work );
	}

	private <T> T doTheWork(WorkExecutorVisitable<T> work) {
		try {
			// obtain our isolated connection
			Connection connection = jdbcConnectionAccess().obtainConnection();
			try {
				// do the actual work
				return work.accept( new WorkExecutor<T>(), connection );
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
				catch ( Throwable ignore ) {
					LOG.unableToReleaseIsolatedConnection( ignore );
				}
			}
		}
		catch ( SQLException e ) {
			throw sqlExceptionHelper().convert( e, "unable to obtain isolated JDBC connection" );
		}
	}
}

