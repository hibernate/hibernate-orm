/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.engine.transaction;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import java.sql.Connection;
import java.sql.SQLException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.jdbc.spi.SQLExceptionHelper;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.exception.SQLExceptionConverter;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Class which provides the isolation semantics required by
 * an {@link IsolatedWork}.  Processing comes in two flavors:<ul>
 * <li>{@link #doIsolatedWork} : makes sure the work to be done is
 * performed in a seperate, distinct transaction</li>
 * <li>{@link #doNonTransactedWork} : makes sure the work to be
 * done is performed outside the scope of any transaction</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class Isolater {

    static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class, Isolater.class.getPackage().getName());

	/**
	 * Ensures that all processing actually performed by the given work will
	 * occur on a seperate transaction.
	 *
	 * @param work The work to be performed.
	 * @param session The session from which this request is originating.
	 * @throws HibernateException
	 */
	public static void doIsolatedWork(IsolatedWork work, SessionImplementor session) throws HibernateException {
		boolean isJta = session.getFactory().getTransactionManager() != null;
		if ( isJta ) {
			new JtaDelegate( session ).delegateWork( work, true );
		}
		else {
			new JdbcDelegate( session ).delegateWork( work, true );
		}
	}

	/**
	 * Ensures that all processing actually performed by the given work will
	 * occur outside of a transaction.
	 *
	 * @param work The work to be performed.
	 * @param session The session from which this request is originating.
	 * @throws HibernateException
	 */
	public static void doNonTransactedWork(IsolatedWork work, SessionImplementor session) throws HibernateException {
		boolean isJta = session.getFactory().getTransactionManager() != null;
		if ( isJta ) {
			new JtaDelegate( session ).delegateWork( work, false );
		}
		else {
			new JdbcDelegate( session ).delegateWork( work, false );
		}
	}

	// should be ok performance-wise to generate new delegate instances for each
	// request since these are locally stack-scoped.  Besides, it makes the code
	// much easier to read than the old TransactionHelper stuff...

	private static interface Delegate {
		public void delegateWork(IsolatedWork work, boolean transacted) throws HibernateException;
	}

	/**
	 * An isolation delegate for JTA-based transactions.  Essentially susepnds
	 * any current transaction, does the work in a new transaction, and then
	 * resumes the initial transaction (if there was one).
	 */
	public static class JtaDelegate implements Delegate {
		private final SessionImplementor session;

		public JtaDelegate(SessionImplementor session) {
			this.session = session;
		}

		public void delegateWork(IsolatedWork work, boolean transacted) throws HibernateException {
			TransactionManager transactionManager = session.getFactory().getTransactionManager();

			try {
				// First we suspend any current JTA transaction
				Transaction surroundingTransaction = transactionManager.suspend();
                LOG.jtaTransactionSuspended(surroundingTransaction);

				boolean hadProblems = false;
				try {
					// then peform the requested work
					if ( transacted ) {
						doTheWorkInNewTransaction( work, transactionManager );
					}
					else {
						doTheWorkInNoTransaction( work );
					}
				}
				catch ( HibernateException e ) {
					hadProblems = true;
					throw e;
				}
				finally {
					try {
						transactionManager.resume( surroundingTransaction );
                        LOG.jtaTransactionResumed(surroundingTransaction);
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

		private void doTheWorkInNewTransaction(IsolatedWork work, TransactionManager transactionManager) {
			try {
				// start the new isolated transaction
				transactionManager.begin();

				try {
					doTheWork( work );
					// if everythign went ok, commit the isolated transaction
					transactionManager.commit();
				}
				catch ( Exception e ) {
					try {
						transactionManager.rollback();
					}
					catch ( Exception ignore ) {
                        LOG.unableToRollbackIsolatedTransaction(e, ignore);
					}
				}
			}
			catch ( SystemException e ) {
				throw new HibernateException( "Unable to start isolated transaction", e );
			}
			catch ( NotSupportedException e ) {
				throw new HibernateException( "Unable to start isolated transaction", e );
			}
		}

		private void doTheWorkInNoTransaction(IsolatedWork work) {
			doTheWork( work );
		}

		private void doTheWork(IsolatedWork work) {
			try {
				// obtain our isolated connection
				Connection connection = session.getFactory().getConnectionProvider().getConnection();
				try {
					// do the actual work
					work.doWork( connection );
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
						session.getFactory().getConnectionProvider().closeConnection( connection );
					}
					catch ( Throwable ignore ) {
                        LOG.unableToReleaseIsolatedConnection(ignore);
					}
				}
			}
			catch ( SQLException sqle ) {
				throw sqlExceptionHelper().convert(
						sqle,
						"unable to obtain isolated JDBC connection"
				);
			}
		}

		private SQLExceptionHelper sqlExceptionHelper() {
			return session.getFactory().getSQLExceptionHelper();
		}
	}

	/**
	 * An isolation delegate for JDBC-based transactions.  Basically just
	 * grabs a new connection and does the work on that.
	 */
	public static class JdbcDelegate implements Delegate {
		private final SessionImplementor session;

		public JdbcDelegate(SessionImplementor session) {
			this.session = session;
		}

		public void delegateWork(IsolatedWork work, boolean transacted) throws HibernateException {
			boolean wasAutoCommit = false;
			try {
				Connection connection = session.getFactory().getConnectionProvider().getConnection();
				try {
					if ( transacted ) {
						if ( connection.getAutoCommit() ) {
							wasAutoCommit = true;
							connection.setAutoCommit( false );
						}
					}

					work.doWork( connection );

					if ( transacted ) {
						connection.commit();
					}
				}
				catch( Exception e ) {
					try {
						if ( transacted && !connection.isClosed() ) {
							connection.rollback();
						}
					}
					catch( Exception ignore ) {
                        LOG.unableToRollbackConnection(ignore);
					}

					if ( e instanceof HibernateException ) {
						throw ( HibernateException ) e;
					}
					else if ( e instanceof SQLException ) {
						throw sqlExceptionHelper().convert(
								( SQLException ) e,
								"error performing isolated work"
						);
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
						catch( Exception ignore ) {
                            LOG.unableToResetConnectionToAutoCommit();
						}
					}
					try {
						session.getFactory().getConnectionProvider().closeConnection( connection );
					}
					catch ( Exception ignore ) {
                        LOG.unableToReleaseIsolatedConnection(ignore);
					}
				}
			}
			catch ( SQLException sqle ) {
				throw sqlExceptionHelper().convert(
						sqle,
						"unable to obtain isolated JDBC connection"
				);
			}
		}

		private SQLExceptionHelper sqlExceptionHelper() {
			return session.getFactory().getSQLExceptionHelper();
		}
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = INFO )
        @Message( value = "On release of batch it still contained JDBC statements" )
        void batchContainedStatementsOnRelease();

        @LogMessage( level = DEBUG )
        @Message( value = "Surrounding JTA transaction resumed [%s]" )
        void jtaTransactionResumed( Transaction surroundingTransaction );

        @LogMessage( level = DEBUG )
        @Message( value = "Surrounding JTA transaction suspended [%s]" )
        void jtaTransactionSuspended( Transaction surroundingTransaction );

        @LogMessage( level = INFO )
        @Message( value = "Unable to release isolated connection [%s]" )
        void unableToReleaseIsolatedConnection( Throwable ignore );

        @LogMessage( level = TRACE )
        @Message( value = "Unable to reset connection back to auto-commit" )
        void unableToResetConnectionToAutoCommit();

        @LogMessage( level = INFO )
        @Message( value = "Unable to rollback connection on exception [%s]" )
        void unableToRollbackConnection( Exception ignore );

        @LogMessage( level = INFO )
        @Message( value = "Unable to rollback isolated transaction on error [%s] : [%s]" )
        void unableToRollbackIsolatedTransaction( Exception e,
                                                  Exception ignore );
    }
}
