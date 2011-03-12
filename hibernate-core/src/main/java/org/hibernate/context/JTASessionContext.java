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
package org.hibernate.context;

import org.hibernate.HibernateException;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.classic.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.util.JTAHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Synchronization;
import java.util.Map;
import java.util.Hashtable;

/**
 * An implementation of {@link CurrentSessionContext} which scopes the notion
 * of a current session to a JTA transaction.  Because JTA gives us a nice
 * tie-in to clean up after ourselves, this implementation will generate
 * Sessions as needed provided a JTA transaction is in effect.  If a session
 * is not already associated with the current JTA transaction at the time
 * {@link #currentSession()} is called, a new session will be opened and it
 * will be associated with that JTA transaction.
 * <p/>
 * Note that the sessions returned from this method are automatically configured with
 * both the {@link org.hibernate.cfg.Environment#FLUSH_BEFORE_COMPLETION auto-flush} and
 * {@link org.hibernate.cfg.Environment#AUTO_CLOSE_SESSION auto-close} attributes set to
 * true, meaning that the Session will be automatically flushed and closed
 * as part of the lifecycle for the JTA transaction to which it is associated.
 * Additionally, it will also be configured to aggressively release JDBC
 * connections after each statement is executed.  These settings are governed
 * by the {@link #isAutoFlushEnabled()}, {@link #isAutoCloseEnabled()}, and
 * {@link #getConnectionReleaseMode()} methods; these are provided (along with
 * the {@link #buildOrObtainSession()} method) for easier subclassing for custom
 * JTA-based session tracking logic (like maybe long-session semantics).
 *
 * @author Steve Ebersole
 */
public class JTASessionContext implements CurrentSessionContext {

	private static final Logger log = LoggerFactory.getLogger( JTASessionContext.class );

	protected final SessionFactoryImplementor factory;
	private transient Map currentSessionMap = new Hashtable();

	public JTASessionContext(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	/**
	 * {@inheritDoc}
	 */
	public Session currentSession() throws HibernateException {
		TransactionManager transactionManager = factory.getTransactionManager();
		if ( transactionManager == null ) {
			throw new HibernateException( "No TransactionManagerLookup specified" );
		}

		Transaction txn;
		try {
			txn = transactionManager.getTransaction();
			if ( txn == null ) {
				throw new HibernateException( "Unable to locate current JTA transaction" );
			}
			if ( !JTAHelper.isInProgress( txn.getStatus() ) ) {
				// We could register the session against the transaction even though it is
				// not started, but we'd have no guarentee of ever getting the map
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

		final Object txnIdentifier = factory.getSettings().getTransactionManagerLookup() == null
				? txn
				: factory.getSettings().getTransactionManagerLookup().getTransactionIdentifier( txn );

		Session currentSession = ( Session ) currentSessionMap.get( txnIdentifier );

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
					log.debug( "Unable to release generated current-session on failed synch registration", ignore );
				}
				throw new HibernateException( "Unable to register cleanup Synchronization with TransactionManager" );
			}

			currentSessionMap.put( txnIdentifier, currentSession );
		}

		return currentSession;
	}

	/**
	 * Builds a {@link CleanupSynch} capable of cleaning up the the current session map as an after transaction
	 * callback.
	 *
	 * @param transactionIdentifier The transaction identifier under which the current session is registered.
	 * @return The cleanup synch.
	 */
	private CleanupSynch buildCleanupSynch(Object transactionIdentifier) {
		return new CleanupSynch( transactionIdentifier, this );
	}

	/**
	 * Strictly provided for subclassing purposes; specifically to allow long-session
	 * support.
	 * <p/>
	 * This implementation always just opens a new session.
	 *
	 * @return the built or (re)obtained session.
	 */
	protected Session buildOrObtainSession() {
		return factory.openSession(
				null,
		        isAutoFlushEnabled(),
		        isAutoCloseEnabled(),
		        getConnectionReleaseMode()
			);
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
	 * JTA transaction synch used for cleanup of the internal session map.
	 */
	protected static class CleanupSynch implements Synchronization {
		private Object transactionIdentifier;
		private JTASessionContext context;

		public CleanupSynch(Object transactionIdentifier, JTASessionContext context) {
			this.transactionIdentifier = transactionIdentifier;
			this.context = context;
		}

		/**
		 * {@inheritDoc}
		 */
		public void beforeCompletion() {
		}

		/**
		 * {@inheritDoc}
		 */
		public void afterCompletion(int i) {
			context.currentSessionMap.remove( transactionIdentifier );
		}
	}
}
