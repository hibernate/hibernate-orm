/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.transaction.Status;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.spi.IsolationDelegate;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.resource.transaction.TransactionRequiredForJoinException;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.RegisteredSynchronization;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinator;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinatorNonTrackingImpl;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinatorTrackingImpl;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackTarget;
import org.hibernate.resource.transaction.internal.SynchronizationRegistryStandardImpl;
import org.hibernate.resource.transaction.spi.SynchronizationRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.resource.transaction.spi.TransactionObserver;
import org.hibernate.resource.transaction.spi.TransactionStatus;


import static java.util.Collections.addAll;
import static java.util.Collections.emptyList;
import static org.hibernate.resource.transaction.backend.jta.internal.JtaLogging.JTA_LOGGER;
import static org.hibernate.resource.transaction.spi.TransactionStatus.ACTIVE;
import static org.hibernate.resource.transaction.spi.TransactionStatus.NOT_ACTIVE;

/**
 * An implementation of TransactionCoordinator based on managing a transaction through the JTA API (either TM or UT)
 *
 * @author Steve Ebersole
 */
public class JtaTransactionCoordinatorImpl implements TransactionCoordinator, SynchronizationCallbackTarget {

	private final @Nonnull TransactionCoordinatorBuilder transactionCoordinatorBuilder;
	private final @Nonnull TransactionCoordinatorOwner transactionCoordinatorOwner;
	private final @Nonnull JtaPlatform jtaPlatform;
	private final boolean autoJoinTransactions;
	private final boolean preferUserTransactions;
	private final boolean performJtaThreadTracking;

	private boolean synchronizationRegistered;
	private @Nullable SynchronizationCallbackCoordinator callbackCoordinator;
	private @Nullable TransactionDriverControlImpl physicalTransactionDelegate;

	private final SynchronizationRegistryStandardImpl synchronizationRegistry =
			new SynchronizationRegistryStandardImpl();

	private int timeOut = -1;

	private transient List<TransactionObserver> observers = null;

	/**
	 * Construct a JtaTransactionCoordinatorImpl instance.  package-protected to ensure access goes through
	 * builder.
	 *
	 * @param owner The transactionCoordinatorOwner
	 * @param autoJoinTransactions Should JTA transactions be auto-joined?  Or should we wait for explicit join calls?
	 */
	JtaTransactionCoordinatorImpl(
			@Nonnull TransactionCoordinatorBuilder transactionCoordinatorBuilder,
			@Nonnull TransactionCoordinatorOwner owner,
			boolean autoJoinTransactions,
			@Nonnull JtaPlatform jtaPlatform) {
		this.transactionCoordinatorBuilder = transactionCoordinatorBuilder;
		this.transactionCoordinatorOwner = owner;
		this.autoJoinTransactions = autoJoinTransactions;
		this.jtaPlatform = jtaPlatform;

		final var jdbcSessionContext = owner.getJdbcSessionOwner().getJdbcSessionContext();
		preferUserTransactions = jdbcSessionContext.isPreferUserTransaction();
		performJtaThreadTracking = jdbcSessionContext.isJtaTrackByThread();

		synchronizationRegistered = false;

		pulse();
	}

	public JtaTransactionCoordinatorImpl(
			@Nonnull TransactionCoordinatorBuilder transactionCoordinatorBuilder,
			@Nonnull TransactionCoordinatorOwner owner,
			boolean autoJoinTransactions,
			@Nonnull JtaPlatform jtaPlatform,
			boolean preferUserTransactions,
			boolean performJtaThreadTracking,
			@Nullable TransactionObserver... observers) {
		this.transactionCoordinatorBuilder = transactionCoordinatorBuilder;
		this.transactionCoordinatorOwner = owner;
		this.autoJoinTransactions = autoJoinTransactions;
		this.jtaPlatform = jtaPlatform;
		this.preferUserTransactions = preferUserTransactions;
		this.performJtaThreadTracking = performJtaThreadTracking;

		if ( observers != null ) {
			this.observers = new ArrayList<>( observers.length );
			addAll( this.observers, observers );
		}

		synchronizationRegistered = false;

		pulse();
	}

	/**
	 * Needed because while iterating the observers list and executing the before/update callbacks,
	 * some observers might get removed from the list.
	 * Yet try to not allocate anything for when the list is empty, as this is a common case.
	 *
	 * @return TransactionObserver
	 */
	@Nonnull
	private Iterable<TransactionObserver> observers() {
		return observers == null ? emptyList() : new ArrayList<>( observers );
	}

	@Nonnull
	public SynchronizationCallbackCoordinator getSynchronizationCallbackCoordinator() {
		if ( callbackCoordinator == null ) {
			callbackCoordinator = performJtaThreadTracking
					? new SynchronizationCallbackCoordinatorTrackingImpl( this )
					: new SynchronizationCallbackCoordinatorNonTrackingImpl( this );
		}
		return callbackCoordinator;
	}

	@Override
	public void pulse() {
		if ( autoJoinTransactions && !synchronizationRegistered ) {
			// Can we register a synchronization according to the JtaPlatform?
			if ( !jtaPlatform.canRegisterSynchronization() ) {
				JTA_LOGGER.cannotRegisterSynchronization();
			}
			else {
				joinJtaTransaction();
			}
		}
	}

	/**
	 * Join to the JTA transaction.  Note that the underlying meaning of joining in JTA environments is to register the
	 * RegisteredSynchronization with the JTA system
	 */
	private void joinJtaTransaction() {
		if ( !synchronizationRegistered ) {
			jtaPlatform.registerSynchronization(
					new RegisteredSynchronization( getSynchronizationCallbackCoordinator() ) );
			getSynchronizationCallbackCoordinator().synchronizationRegistered();
			synchronizationRegistered = true;
			JTA_LOGGER.registeredSynchronization();
			// report entering into a "transactional context"
			getTransactionCoordinatorOwner().startTransactionBoundary();
		}
	}

	@Override
	public void explicitJoin() {
		if ( synchronizationRegistered ) {
			JTA_LOGGER.alreadyJoinedJtaTransaction();
		}
		else {
			if ( getTransactionDriverControl().getStatus() != ACTIVE ) {
				throw new TransactionRequiredForJoinException(
						"Explicitly joining a JTA transaction requires a JTA transaction be currently active"
				);
			}
			joinJtaTransaction();
		}
	}

	@Override
	public boolean isJoined() {
		return synchronizationRegistered;
	}

	/**
	 * Is the RegisteredSynchronization used by Hibernate for unified JTA Synchronization callbacks registered for this
	 * coordinator?
	 *
	 * @return {@code true} indicates that a RegisteredSynchronization is currently registered for this coordinator;
	 * {@code false} indicates it is not (yet) registered.
	 */
	public boolean isSynchronizationRegistered() {
		return synchronizationRegistered;
	}

	@Nonnull
	public TransactionCoordinatorOwner getTransactionCoordinatorOwner(){
		return transactionCoordinatorOwner;
	}

	@Override
	@Nonnull
	public JpaCompliance getJpaCompliance() {
		return transactionCoordinatorOwner.getJdbcSessionOwner().getJdbcSessionContext().getJpaCompliance();
	}

	@Override
	@Nonnull
	public TransactionDriver getTransactionDriverControl() {
		if ( physicalTransactionDelegate == null ) {
			physicalTransactionDelegate = makePhysicalTransactionDelegate();
		}
		return physicalTransactionDelegate;
	}

	@Nonnull
	private TransactionDriverControlImpl makePhysicalTransactionDelegate() {
		final var adapter =
				preferUserTransactions
						? getTransactionAdapterPreferringUserTransaction()
						: getTransactionAdapterPreferringTransactionManager();
		if ( adapter == null ) {
			throw new JtaPlatformInaccessibleException(
					"Unable to access TransactionManager or UserTransaction to make physical transaction delegate"
			);
		}
		else {
			return new TransactionDriverControlImpl( adapter );
		}
	}

	@Nullable
	private JtaTransactionAdapter getTransactionAdapterPreferringTransactionManager() {
		final var adapter = makeTransactionManagerAdapter();
		if ( adapter == null ) {
			JTA_LOGGER.unableToAccessTransactionManagerTryingUserTransaction();
			return makeUserTransactionAdapter();
		}
		else {
			return adapter;
		}
	}

	@Nullable
	private JtaTransactionAdapter getTransactionAdapterPreferringUserTransaction() {
		final var adapter = makeUserTransactionAdapter();
		if ( adapter == null ) {
			JTA_LOGGER.unableToAccessUserTransactionTryingTransactionManager();
			return makeTransactionManagerAdapter();
		}
		else {
			return adapter;
		}
	}

	@Nullable
	private JtaTransactionAdapter makeUserTransactionAdapter() {
		try {
			final var userTransaction = jtaPlatform.retrieveUserTransaction();
			if ( userTransaction == null ) {
				JTA_LOGGER.userTransactionReturnedNull();
				return null;
			}
			else {
				return new JtaTransactionAdapterUserTransactionImpl( userTransaction );
			}
		}
		catch ( Exception exception ) {
			JTA_LOGGER.exceptionRetrievingUserTransaction( exception.getMessage(), exception );
			return null;
		}
	}

	@Nullable
	private JtaTransactionAdapter makeTransactionManagerAdapter() {
		try {
			final var transactionManager = jtaPlatform.retrieveTransactionManager();
			if ( transactionManager == null ) {
				JTA_LOGGER.transactionManagerReturnedNull();
				return null;
			}
			else {
				return new JtaTransactionAdapterTransactionManagerImpl( transactionManager );
			}
		}
		catch ( Exception exception ) {
			JTA_LOGGER.exceptionRetrievingTransactionManager( exception.getMessage(), exception );
			return null;
		}
	}

	@Override
	@Nonnull
	public SynchronizationRegistry getLocalSynchronizations() {
		return synchronizationRegistry;
	}

	@Override
	public boolean isActive() {
		return transactionCoordinatorOwner.isActive();
	}

	public boolean isJtaTransactionCurrentlyActive() {
		return getTransactionDriverControl().getStatus() == ACTIVE;
	}

	@Override
	@Nonnull
	public IsolationDelegate createIsolationDelegate() {
		return new JtaIsolationDelegate( transactionCoordinatorOwner,
				jtaPlatform.retrieveTransactionManager() );
	}

	@Override
	@Nonnull
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
		return transactionCoordinatorBuilder;
	}

	@Override
	public void setTimeOut(int seconds) {
		this.timeOut = seconds;
		physicalTransactionDelegate.jtaTransactionAdapter.setTimeOut( seconds );
	}

	@Override
	public int getTimeOut() {
		return timeOut;
	}

	@Override
	public void invalidate() {
		if ( physicalTransactionDelegate != null ) {
			physicalTransactionDelegate.invalidate();
		}
		physicalTransactionDelegate = null;
	}

	// SynchronizationCallbackTarget ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void beforeCompletion() {
		JTA_LOGGER.notifyingJtaObserversBeforeCompletion();
		try {
			transactionCoordinatorOwner.beforeTransactionCompletion();
		}
		catch ( Exception e ) {
			physicalTransactionDelegate.markRollbackOnly();
			throw e;
		}
		finally {
			synchronizationRegistry.notifySynchronizationsBeforeTransactionCompletion();
			for ( var transactionObserver : observers() ) {
				transactionObserver.beforeCompletion();
			}
		}
	}

	@Override
	public void afterCompletion(boolean successful, boolean delayed) {
		if ( transactionCoordinatorOwner.isActive() ) {
			JTA_LOGGER.notifyingJtaObserversAfterCompletion();

			final int statusToSend = successful ? Status.STATUS_COMMITTED : Status.STATUS_UNKNOWN;
			synchronizationRegistry.notifySynchronizationsAfterTransactionCompletion( statusToSend );

//			afterCompletionAction.doAction( this, statusToSend );

			transactionCoordinatorOwner.afterTransactionCompletion( successful, delayed );

			for ( var transactionObserver : observers() ) {
				transactionObserver.afterCompletion( successful, delayed );
			}

			synchronizationRegistered = false;
		}
	}

	public void addObserver(@Nonnull TransactionObserver observer) {
		if ( observers == null ) {
			observers = new ArrayList<>( 3 ); //These lists are typically very small.
		}
		observers.add( observer );
	}

	@Override
	public void removeObserver(@Nonnull TransactionObserver observer) {
		if ( observers != null ) {
			observers.remove( observer );
		}
	}

	/**
	 * Implementation of the LocalInflow for this TransactionCoordinator.  Allows the
	 * local transaction ({@link org.hibernate.Transaction}) to callback into this
	 * TransactionCoordinator for the purpose of driving the underlying JTA transaction.
	 */
	public class TransactionDriverControlImpl implements TransactionDriver {
		private final @Nonnull JtaTransactionAdapter jtaTransactionAdapter;
		private boolean invalid;

		public TransactionDriverControlImpl(@Nonnull JtaTransactionAdapter jtaTransactionAdapter) {
			this.jtaTransactionAdapter = jtaTransactionAdapter;
		}

		protected void invalidate() {
			invalid = true;
		}

		@Override
		public void begin() {
			errorIfInvalid();
			jtaTransactionAdapter.begin();
			joinJtaTransaction();
		}

		protected void errorIfInvalid() {
			if ( invalid ) {
				throw new IllegalStateException( "Physical-transaction delegate is no longer valid" );
			}
		}

		@Override
		public void commit() {
			errorIfInvalid();
			getTransactionCoordinatorOwner().flushBeforeTransactionCompletion();
			// we don't have to perform any before/after completion processing here.  We leave that for
			// the Synchronization callbacks
			jtaTransactionAdapter.commit();
		}

		@Override
		public void rollback() {
			errorIfInvalid();
			// we don't have to perform any after completion processing here.  We leave that for
			// the Synchronization callbacks
			jtaTransactionAdapter.rollback();
		}

		@Override
		@Nonnull
		public TransactionStatus getStatus() {
			return jtaTransactionAdapter.getStatus();
		}

		@Override
		public void markRollbackOnly() {
			if ( jtaTransactionAdapter.getStatus() != NOT_ACTIVE  ) {
				jtaTransactionAdapter.markRollbackOnly();
			}
		}
	}

}
