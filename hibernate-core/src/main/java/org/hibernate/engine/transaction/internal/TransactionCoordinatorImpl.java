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
package org.hibernate.engine.transaction.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.ResourceClosedException;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.SynchronizationRegistry;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionEnvironment;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.engine.transaction.synchronization.internal.RegisteredSynchronization;
import org.hibernate.engine.transaction.synchronization.internal.SynchronizationCallbackCoordinatorNonTrackingImpl;
import org.hibernate.engine.transaction.synchronization.internal.SynchronizationCallbackCoordinatorTrackingImpl;
import org.hibernate.engine.transaction.synchronization.spi.SynchronizationCallbackCoordinator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * Standard implementation of the Hibernate {@link TransactionCoordinator}
 * <p/>
 * IMPL NOTE : Custom serialization handling!
 *
 * @author Steve Ebersole
 */
public final class TransactionCoordinatorImpl implements TransactionCoordinator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( TransactionCoordinatorImpl.class );

	private final transient TransactionContext transactionContext;
	private final transient JdbcCoordinatorImpl jdbcCoordinator;
	private final transient TransactionFactory transactionFactory;
	private final transient TransactionEnvironment transactionEnvironment;

	private final transient List<TransactionObserver> observers;
	private final transient SynchronizationRegistryImpl synchronizationRegistry;

	private transient TransactionImplementor currentHibernateTransaction;

	private transient SynchronizationCallbackCoordinator callbackCoordinator;

	private transient boolean open = true;
	private transient boolean synchronizationRegistered;
	private transient boolean ownershipTaken;
	
	private transient boolean isDebugging = LOG.isDebugEnabled();
	private transient boolean isTracing = LOG.isTraceEnabled();

	public TransactionCoordinatorImpl(
			Connection userSuppliedConnection,
			TransactionContext transactionContext) {
		this.transactionContext = transactionContext;
		this.jdbcCoordinator = new JdbcCoordinatorImpl( userSuppliedConnection, this );
		this.transactionEnvironment = transactionContext.getTransactionEnvironment();
		this.transactionFactory = this.transactionEnvironment.getTransactionFactory();
		this.observers = new ArrayList<TransactionObserver>();
		this.synchronizationRegistry = new SynchronizationRegistryImpl();
		reset();

		final boolean registerSynchronization = transactionContext.isAutoCloseSessionEnabled()
				|| transactionContext.isFlushBeforeCompletionEnabled()
				|| transactionContext.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_TRANSACTION;
		if ( registerSynchronization ) {
			pulse();
		}
	}

	public TransactionCoordinatorImpl(
			TransactionContext transactionContext,
			JdbcCoordinatorImpl jdbcCoordinator,
			List<TransactionObserver> observers) {
		this.transactionContext = transactionContext;
		this.jdbcCoordinator = jdbcCoordinator;
		this.transactionEnvironment = transactionContext.getTransactionEnvironment();
		this.transactionFactory = this.transactionEnvironment.getTransactionFactory();
		this.observers = observers;
		this.synchronizationRegistry = new SynchronizationRegistryImpl();
		reset();
	}

	/**
	 * Reset the internal state.
	 */
	public void reset() {
		synchronizationRegistered = false;
		ownershipTaken = false;

		if ( currentHibernateTransaction != null ) {
			currentHibernateTransaction.invalidate();
		}
		currentHibernateTransaction = transactionFactory().createTransaction( this );
		if ( transactionContext.shouldAutoJoinTransaction() ) {
			currentHibernateTransaction.markForJoin();
			currentHibernateTransaction.join();
		}

		// IMPL NOTE : reset clears synchronizations (following jta spec), but not observers!
		synchronizationRegistry.clearSynchronizations();
	}

	public void afterTransaction(TransactionImplementor hibernateTransaction, int status) {
		if (isTracing) {
			LOG.trace( "after transaction completion" );
		}

		final boolean success = JtaStatusHelper.isCommitted( status );

		if ( sessionFactory().getStatistics().isStatisticsEnabled() ) {
			transactionEnvironment.getStatisticsImplementor().endTransaction( success );
		}

		getJdbcCoordinator().afterTransaction();

		getTransactionContext().afterTransactionCompletion( hibernateTransaction, success );
		sendAfterTransactionCompletionNotifications( hibernateTransaction, status );
		reset();
	}

	private SessionFactoryImplementor sessionFactory() {
		return transactionEnvironment.getSessionFactory();
	}

	public boolean isSynchronizationRegistered() {
		return synchronizationRegistered;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public boolean isTransactionInProgress() {
		return open && getTransaction().isActive() && getTransaction().getJoinStatus() == JoinStatus.JOINED;
	}

	@Override
	public TransactionContext getTransactionContext() {
		return transactionContext;
	}

	@Override
	public JdbcCoordinator getJdbcCoordinator() {
		return jdbcCoordinator;
	}

	private TransactionFactory transactionFactory() {
		return transactionFactory;
	}

	private TransactionEnvironment getTransactionEnvironment() {
		return transactionEnvironment;
	}

	@Override
	public TransactionImplementor getTransaction() {
		if ( !open ) {
			throw new ResourceClosedException( "This TransactionCoordinator has been closed" );
		}
		pulse();
		return currentHibernateTransaction;
	}

	public void afterNonTransactionalQuery(boolean success) {
		// check to see if the connection is in auto-commit mode (no connection means aggressive connection
		// release outside a JTA transaction context, so MUST be autocommit mode)
		boolean isAutocommit = getJdbcCoordinator().getLogicalConnection().isAutoCommit();
		getJdbcCoordinator().afterTransaction();

		if ( isAutocommit ) {
			for ( TransactionObserver observer : observers ) {
				observer.afterCompletion( success, this.getTransaction() );
			}
		}
	}

	@Override
	public void resetJoinStatus() {
		getTransaction().resetJoinStatus();
	}

	@SuppressWarnings({"unchecked"})
	private void attemptToRegisterJtaSync() {
		if ( synchronizationRegistered ) {
			return;
		}

		final JtaPlatform jtaPlatform = getTransactionEnvironment().getJtaPlatform();
		if ( jtaPlatform == null ) {
			// if no jta platform was registered we wont be able to register a jta synchronization
			return;
		}

		// Has the local transaction (Hibernate facade) taken on the responsibility of driving the transaction inflow?
		if ( currentHibernateTransaction.isInitiator() ) {
			return;
		}

		final JoinStatus joinStatus = currentHibernateTransaction.getJoinStatus();
		if ( joinStatus != JoinStatus.JOINED ) {
			// the transaction is not (yet) joined, see if we should join...
			if ( !transactionContext.shouldAutoJoinTransaction() ) {
				// we are supposed to not auto join transactions; if the transaction is not marked for join
				// we cannot go any further in attempting to join (register sync).
				if ( joinStatus != JoinStatus.MARKED_FOR_JOINED ) {
					if (isDebugging) {
						LOG.debug( "Skipping JTA sync registration due to auto join checking" );
					}
					return;
				}
			}
		}

		// IMPL NOTE : At this point the local callback is the "maybe" one.  The only time that needs to change is if
		// we are able to successfully register the transaction synchronization in which case the local callback would become
		// non driving.  To that end, the following checks are simply opt outs where we are unable to register the
		// synchronization

		// Can we resister a synchronization
		if ( !jtaPlatform.canRegisterSynchronization() ) {
			if (isTracing) {
				LOG.trace( "registered JTA platform says we cannot currently register synchronization; skipping" );
			}
			return;
		}

		// Should we resister a synchronization
		if ( !transactionFactory().isJoinableJtaTransaction( this, currentHibernateTransaction ) ) {
			if (isTracing) {
				LOG.trace( "TransactionFactory reported no JTA transaction to join; skipping Synchronization registration" );
			}
			return;
		}

		jtaPlatform.registerSynchronization( new RegisteredSynchronization( getSynchronizationCallbackCoordinator() ) );
		getSynchronizationCallbackCoordinator().synchronizationRegistered();
		synchronizationRegistered = true;
		if (isDebugging) {
			LOG.debug( "successfully registered Synchronization" );
		}
	}

	@Override
	public SynchronizationCallbackCoordinator getSynchronizationCallbackCoordinator() {
		if ( callbackCoordinator == null ) {
			callbackCoordinator = transactionEnvironment.getSessionFactory().getSettings().isJtaTrackByThread()
					? new SynchronizationCallbackCoordinatorTrackingImpl( this )
					: new SynchronizationCallbackCoordinatorNonTrackingImpl( this );
		}
		return callbackCoordinator;
	}

	public void pulse() {
		if ( transactionFactory().compatibleWithJtaSynchronization() ) {
			// the configured transaction strategy says it supports callbacks via JTA synchronization, so attempt to
			// register JTA synchronization if possible
			attemptToRegisterJtaSync();
		}
	}

	public Connection close() {
		open = false;
		reset();
		observers.clear();
		return jdbcCoordinator.close();
	}

	public SynchronizationRegistry getSynchronizationRegistry() {
		return synchronizationRegistry;
	}

	public void addObserver(TransactionObserver observer) {
		observers.add( observer );
	}

	@Override
	public void removeObserver(TransactionObserver observer) {
		observers.remove( observer );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public boolean isTransactionJoinable() {
		return transactionFactory().isJoinableJtaTransaction( this, currentHibernateTransaction );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public boolean isTransactionJoined() {
		return currentHibernateTransaction != null && currentHibernateTransaction.getJoinStatus() == JoinStatus.JOINED;
	}

	public void setRollbackOnly() {
		getTransaction().markRollbackOnly();
	}

	@Override
	public boolean takeOwnership() {
		if ( ownershipTaken ) {
			return false;
		}
		else {
			ownershipTaken = true;
			return true;
		}
	}

	@Override
	public void sendAfterTransactionBeginNotifications(TransactionImplementor hibernateTransaction) {
		for ( TransactionObserver observer : observers ) {
			observer.afterBegin( currentHibernateTransaction );
		}
	}

	@Override
	public void sendBeforeTransactionCompletionNotifications(TransactionImplementor hibernateTransaction) {
		synchronizationRegistry.notifySynchronizationsBeforeTransactionCompletion();
		for ( TransactionObserver observer : observers ) {
			observer.beforeCompletion( hibernateTransaction );
		}
	}

	@Override
	public void sendAfterTransactionCompletionNotifications(TransactionImplementor hibernateTransaction, int status) {
		final boolean successful = JtaStatusHelper.isCommitted( status );
		for ( TransactionObserver observer : new ArrayList<TransactionObserver>( observers ) ) {
			observer.afterCompletion( successful, hibernateTransaction );
		}
		synchronizationRegistry.notifySynchronizationsAfterTransactionCompletion( status );
	}

	@Override
	public boolean isActive() {
		return !sessionFactory().isClosed();
	}


	// serialization ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void serialize(ObjectOutputStream oos) throws IOException {
		jdbcCoordinator.serialize( oos );
		oos.writeInt( observers.size() );
		for ( TransactionObserver observer : observers ) {
			oos.writeObject( observer );
		}
	}

	public static TransactionCoordinatorImpl deserialize(
			ObjectInputStream ois,
			TransactionContext transactionContext) throws ClassNotFoundException, IOException {
		final JdbcCoordinatorImpl jdbcCoordinator = JdbcCoordinatorImpl.deserialize( ois, transactionContext );
		final int observerCount = ois.readInt();
		final List<TransactionObserver> observers = CollectionHelper.arrayList( observerCount );
		for ( int i = 0; i < observerCount; i++ ) {
			observers.add( (TransactionObserver) ois.readObject() );
		}
		final TransactionCoordinatorImpl transactionCoordinator = new TransactionCoordinatorImpl(
				transactionContext,
				jdbcCoordinator,
				observers
		);
		jdbcCoordinator.afterDeserialize( transactionCoordinator );
		return transactionCoordinator;
	}

}
