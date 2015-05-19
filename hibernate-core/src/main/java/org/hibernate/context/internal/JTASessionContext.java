/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.context.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.context.spi.AbstractCurrentSessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * An implementation of {@link org.hibernate.context.spi.CurrentSessionContext} which scopes the notion
 * of a current session to a JTA transaction.  Because JTA gives us a nice tie-in to clean up after
 * ourselves, this implementation will generate Sessions as needed provided a JTA transaction is in
 * effect.  If a session is not already associated with the current JTA transaction at the time
 * {@link #currentSession()} is called, a new session will be opened and it will be associated with that
 * JTA transaction.
 *
 * Note that the sessions returned from this method are automatically configured with both the
 * {@link org.hibernate.cfg.Environment#FLUSH_BEFORE_COMPLETION auto-flush} and
 * {@link org.hibernate.cfg.Environment#AUTO_CLOSE_SESSION auto-close} attributes set to true, meaning
 * that the Session will be automatically flushed and closed as part of the lifecycle for the JTA
 * transaction to which it is associated.  Additionally, it will also be configured to aggressively
 * release JDBC connections after each statement is executed.  These settings are governed by the
 * {@link #isAutoFlushEnabled()}, {@link #isAutoCloseEnabled()}, and {@link #getConnectionReleaseMode()}
 * methods; these are provided (along with the {@link #buildOrObtainSession()} method) for easier
 * subclassing for custom JTA-based session tracking logic (like maybe long-session semantics).
 *
 * @author Steve Ebersole
 */
public class JTASessionContext extends AbstractCurrentSessionContext {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			JTASessionContext.class.getName()
	);

	private transient Map<Object, Session> currentSessionMap = new ConcurrentHashMap<Object, Session>();

	/**
	 * Constructs a JTASessionContext
	 *
	 * @param factory The factory this context will service
	 */
	public JTASessionContext(SessionFactoryImplementor factory) {
		super( factory );
	}

	@Override
	public Session currentSession() throws HibernateException {
		final JtaPlatform jtaPlatform = factory().getServiceRegistry().getService( JtaPlatform.class );
		final TransactionManager transactionManager = jtaPlatform.retrieveTransactionManager();
		if ( transactionManager == null ) {
			throw new HibernateException( "No TransactionManagerLookup specified" );
		}

		Transaction txn;
		try {
			txn = transactionManager.getTransaction();
			if ( txn == null ) {
				throw new HibernateException( "Unable to locate current JTA transaction" );
			}
			if ( !JtaStatusHelper.isActive( txn.getStatus() ) ) {
				// We could register the session against the transaction even though it is
				// not started, but we'd have no guarantee of ever getting the map
				// entries cleaned up (aside from spawning threads).
				throw new HibernateException( "Current transaction is not in progress" );
			}
		}
		catch ( HibernateException e ) {
			throw e;
		}
		catch ( Throwable t ) {
			throw new HibernateException( "Problem locating/validating JTA transaction", t );
		}

		final Object txnIdentifier = jtaPlatform.getTransactionIdentifier( txn );

		Session currentSession = currentSessionMap.get( txnIdentifier );

		if ( currentSession == null ) {
			currentSession = buildOrObtainSession();

			try {
				txn.registerSynchronization( buildCleanupSynch( txnIdentifier ) );
			}
			catch ( Throwable t ) {
				try {
					currentSession.close();
				}
				catch ( Throwable ignore ) {
					LOG.debug( "Unable to release generated current-session on failed synch registration", ignore );
				}
				throw new HibernateException( "Unable to register cleanup Synchronization with TransactionManager" );
			}

			currentSessionMap.put( txnIdentifier, currentSession );
		}
		else {
			validateExistingSession( currentSession );
		}

		return currentSession;
	}

	/**
	 * Builds a {@link org.hibernate.context.internal.JTASessionContext.CleanupSync} capable of cleaning up the the current session map as an after transaction
	 * callback.
	 *
	 * @param transactionIdentifier The transaction identifier under which the current session is registered.
	 * @return The cleanup synch.
	 */
	private CleanupSync buildCleanupSynch(Object transactionIdentifier) {
		return new CleanupSync( transactionIdentifier, this );
	}

	/**
	 * Strictly provided for subclassing purposes; specifically to allow long-session
	 * support.  This implementation always just opens a new session.
	 *
	 * @return the built or (re)obtained session.
	 */
	@SuppressWarnings("deprecation")
	protected Session buildOrObtainSession() {
		return baseSessionBuilder()
				.autoClose( isAutoCloseEnabled() )
				.connectionReleaseMode( getConnectionReleaseMode() )
				.flushBeforeCompletion( isAutoFlushEnabled() )
				.openSession();
	}

	/**
	 * Mainly for subclass usage.  This impl always returns true.
	 *
	 * @return Whether or not the the session should be closed by transaction completion.
	 */
	protected boolean isAutoCloseEnabled() {
		return true;
	}

	/**
	 * Mainly for subclass usage.  This impl always returns true.
	 *
	 * @return Whether or not the the session should be flushed prior transaction completion.
	 */
	protected boolean isAutoFlushEnabled() {
		return true;
	}

	/**
	 * Mainly for subclass usage.  This impl always returns after_statement.
	 *
	 * @return The connection release mode for any built sessions.
	 */
	protected ConnectionReleaseMode getConnectionReleaseMode() {
		return ConnectionReleaseMode.AFTER_STATEMENT;
	}

	/**
	 * JTA transaction sync used for cleanup of the internal session map.
	 */
	protected static class CleanupSync implements Synchronization {
		private Object transactionIdentifier;
		private JTASessionContext context;

		public CleanupSync(Object transactionIdentifier, JTASessionContext context) {
			this.transactionIdentifier = transactionIdentifier;
			this.context = context;
		}

		@Override
		public void beforeCompletion() {
		}

		@Override
		public void afterCompletion(int i) {
			context.currentSessionMap.remove( transactionIdentifier );
		}
	}
}
