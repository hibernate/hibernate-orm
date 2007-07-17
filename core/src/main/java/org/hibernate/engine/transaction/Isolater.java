package org.hibernate.engine.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;

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

	private static final Log log = LogFactory.getLog( Isolater.class );

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
			Transaction surroundingTransaction = null;
			Connection connection = null;
			boolean caughtException = false;

			try {
				// First we need to suspend any current JTA transaction and obtain
				// a JDBC connection
				surroundingTransaction = transactionManager.suspend();
				if ( log.isDebugEnabled() ) {
					log.debug( "surrounding JTA transaction suspended [" + surroundingTransaction + "]" );
				}

				if ( transacted ) {
					transactionManager.begin();
				}

				connection = session.getBatcher().openConnection();

				// perform the actual work
				work.doWork( connection );

				// if everything went ok, commit the transaction and close the obtained
				// connection handle...
				session.getBatcher().closeConnection( connection );

				if ( transacted ) {
					transactionManager.commit();
				}
			}
			catch( Throwable t ) {
				// at some point the processing went bad, so we need to:
				//      1) make sure the connection handle gets released
				//      2) try to cleanup the JTA context as much as possible
				caughtException = true;
				try {
					if ( connection != null && !connection.isClosed() ) {
						session.getBatcher().closeConnection( connection );
					}
				}
				catch( Throwable ignore ) {
					log.trace( "unable to release connection on exception [" + ignore + "]" );
				}
				if ( transacted ) {
					try {
						transactionManager.rollback();
					}
					catch( Throwable ignore ) {
						log.trace( "unable to rollback new transaction on exception [" + ignore + "]" );
					}
				}
				// finally handle the exception
				if ( t instanceof HibernateException ) {
					throw ( HibernateException ) t;
				}
				else {
					throw new HibernateException( "error performing isolated work", t );
				}
			}
			finally {
				if ( surroundingTransaction != null ) {
					try {
						transactionManager.resume( surroundingTransaction );
						if ( log.isDebugEnabled() ) {
							log.debug( "surrounding JTA transaction resumed [" + surroundingTransaction + "]" );
						}
					}
					catch( Throwable t ) {
						if ( !caughtException ) {
							throw new HibernateException( "unable to resume previously suspended transaction", t );
						}
					}
				}
			}
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
			Connection connection = null;
			boolean wasAutoCommit = false;
			try {
				connection = session.getBatcher().openConnection();

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
			catch( Throwable t ) {
				try {
					if ( transacted && connection != null && !connection.isClosed() ) {
						connection.rollback();
					}
				}
				catch( Throwable ignore ) {
					log.trace( "unable to release connection on exception [" + ignore + "]" );
				}

				if ( t instanceof HibernateException ) {
					throw ( HibernateException ) t;
				}
				else if ( t instanceof SQLException ) {
					throw JDBCExceptionHelper.convert(
							session.getFactory().getSQLExceptionConverter(),
					        ( SQLException ) t,
					        "error performing isolated work"
					);
				}
				else {
					throw new HibernateException( "error performing isolated work", t );
				}
			}
			finally {
				if ( transacted && wasAutoCommit ) {
					try {
						connection.setAutoCommit( true );
					}
					catch( Throwable ignore ) {
						log.trace( "was unable to reset connection back to auto-commit" );
					}
				}
				session.getBatcher().closeConnection( connection );
			}
		}
	}
}
