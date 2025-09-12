/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jdbc.internal;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.RollbackException;
import jakarta.transaction.Status;

import org.hibernate.resource.transaction.spi.IsolationDelegate;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransactionAccess;
import org.hibernate.resource.transaction.internal.SynchronizationRegistryStandardImpl;
import org.hibernate.resource.transaction.spi.SynchronizationRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.resource.transaction.spi.TransactionObserver;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import static java.util.Collections.emptyList;
import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_LOGGER;
import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_MESSAGE_LOGGER;

/**
 * An implementation of {@link TransactionCoordinator} based on managing a
 * transaction through the JDBC {@link Connection} via {@link JdbcResourceTransaction}.
 *
 * @author Steve Ebersole
 *
 * @see JdbcResourceTransaction
 */
public class JdbcResourceLocalTransactionCoordinatorImpl implements TransactionCoordinator {

	private final TransactionCoordinatorBuilder transactionCoordinatorBuilder;
	private final JdbcResourceTransactionAccess jdbcResourceTransactionAccess;
	private final TransactionCoordinatorOwner transactionCoordinatorOwner;
	private final SynchronizationRegistryStandardImpl synchronizationRegistry = new SynchronizationRegistryStandardImpl();

	private final JpaCompliance jpaCompliance;

	private TransactionDriverControlImpl physicalTransactionDelegate;

	private int timeOut = -1;

	private transient List<TransactionObserver> observers = null;

	/**
	 * Construct a ResourceLocalTransactionCoordinatorImpl instance.  package-protected to ensure access goes through
	 * builder.
	 *
	 * @param owner The transactionCoordinatorOwner
	 */
	JdbcResourceLocalTransactionCoordinatorImpl(
			TransactionCoordinatorBuilder transactionCoordinatorBuilder,
			TransactionCoordinatorOwner owner,
			JdbcResourceTransactionAccess jdbcResourceTransactionAccess) {
		this.transactionCoordinatorBuilder = transactionCoordinatorBuilder;
		this.jdbcResourceTransactionAccess = jdbcResourceTransactionAccess;
		this.transactionCoordinatorOwner = owner;
		this.jpaCompliance = owner.getJdbcSessionOwner().getJdbcSessionContext().getJpaCompliance();
	}

	/**
	 * Needed because while iterating the observers list and executing the before/update callbacks,
	 * some observers might get removed from the list.
	 *
	 * @return TransactionObserver
	 */
	private Iterable<TransactionObserver> observers() {
		return observers == null || observers.isEmpty() ? emptyList() : new ArrayList<>( observers );
	}

	@Override
	public TransactionDriver getTransactionDriverControl() {
		// Again, this PhysicalTransactionDelegate will act as the bridge from the local transaction back into the
		// coordinator.  We lazily build it as we invalidate each delegate after each transaction (a delegate is
		// valid for just one transaction)
		if ( physicalTransactionDelegate == null ) {
			physicalTransactionDelegate =
					new TransactionDriverControlImpl( jdbcResourceTransactionAccess.getResourceLocalTransaction() );
		}
		return physicalTransactionDelegate;
	}

	@Override
	public void explicitJoin() {
		// nothing to do here, but log a warning
		JDBC_MESSAGE_LOGGER.callingJoinTransactionOnNonJtaEntityManager();
	}

	@Override
	public boolean isJoined() {
		return physicalTransactionDelegate != null
			&& getTransactionDriverControl().isActive();
	}

	@Override
	public void pulse() {
		// nothing to do here
	}

	@Override
	public SynchronizationRegistry getLocalSynchronizations() {
		return synchronizationRegistry;
	}

	@Override
	public JpaCompliance getJpaCompliance() {
		return jpaCompliance;
	}

	@Override
	public boolean isActive() {
		return transactionCoordinatorOwner.isActive();
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		return new JdbcIsolationDelegate( transactionCoordinatorOwner );
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
		return transactionCoordinatorBuilder;
	}

	@Override
	public void setTimeOut(int seconds) {
		this.timeOut = seconds;
	}

	@Override
	public int getTimeOut() {
		return timeOut;
	}

	// PhysicalTransactionDelegate ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void afterBeginCallback() {
		if ( timeOut > 0 ) {
			transactionCoordinatorOwner.setTransactionTimeOut( timeOut );
		}

		// report entering into a "transactional context"
		transactionCoordinatorOwner.startTransactionBoundary();

		JDBC_MESSAGE_LOGGER.notifyingResourceLocalObserversAfterBegin();

		// trigger the Transaction-API-only after-begin callback
		transactionCoordinatorOwner.afterTransactionBegin();

		// notify all registered observers
		for ( TransactionObserver observer : observers() ) {
			observer.afterBegin();
		}
	}

	private void beforeCompletionCallback() {
		JDBC_MESSAGE_LOGGER.notifyingResourceLocalObserversBeforeCompletion();
		try {
			transactionCoordinatorOwner.beforeTransactionCompletion();
			synchronizationRegistry.notifySynchronizationsBeforeTransactionCompletion();
			for ( var transactionObserver : observers() ) {
				transactionObserver.beforeCompletion();
			}
		}
		catch (RuntimeException e) {
			if ( physicalTransactionDelegate != null ) {
				// should never happen that the physicalTransactionDelegate is null, but to be safe
				physicalTransactionDelegate.markRollbackOnly();
			}
			throw e;
		}
	}

	private void afterCompletionCallback(boolean successful) {
		JDBC_MESSAGE_LOGGER.notifyingResourceLocalObserversAfterCompletion();
		final int statusToSend = successful ? Status.STATUS_COMMITTED : Status.STATUS_ROLLEDBACK;
		synchronizationRegistry.notifySynchronizationsAfterTransactionCompletion( statusToSend );
		transactionCoordinatorOwner.afterTransactionCompletion( successful, false );
		for ( var transactionObserver : observers() ) {
			transactionObserver.afterCompletion( successful, false );
		}
	}

	@Override
	public void addObserver(TransactionObserver observer) {
		if ( observers == null ) {
			observers = new ArrayList<>( 6 );
		}
		observers.add( observer );
	}

	@Override
	public void removeObserver(TransactionObserver observer) {
		if ( observers != null ) {
			observers.remove( observer );
		}
	}

	/**
	 * The delegate bridging between the local (application facing) transaction and the "physical" notion of a
	 * transaction via the JDBC Connection.
	 */
	public class TransactionDriverControlImpl implements TransactionDriver {
		private final JdbcResourceTransaction jdbcResourceTransaction;
		private boolean invalid;
		private boolean rollbackOnly = false;

		public TransactionDriverControlImpl(JdbcResourceTransaction jdbcResourceTransaction) {
			this.jdbcResourceTransaction = jdbcResourceTransaction;
		}

		protected void invalidate() {
			invalid = true;
		}

		@Override
		public void begin() {
			errorIfInvalid();
			jdbcResourceTransaction.begin();
			JdbcResourceLocalTransactionCoordinatorImpl.this.afterBeginCallback();
		}

		protected void errorIfInvalid() {
			if ( invalid ) {
				throw new IllegalStateException( "Physical transaction delegate is no longer valid" );
			}
		}

		@Override
		public void commit() {
			if ( rollbackOnly ) {
				commitRollbackOnly();
			}
			else {
				commitNoRollbackOnly();
			}
		}

		private void commitNoRollbackOnly() {
			try {
				beforeCompletionCallback();
				jdbcResourceTransaction.commit();
				afterCompletionCallback( true );
			}
			catch (RollbackException e) {
				afterCompletionCallback( false );
				throw e;
			}
			catch (RuntimeException e) {
				// something went wrong, so make a last-ditch,
				// hail-mary attempt to roll back the transaction
				try {
					rollback();
				}
				catch (RuntimeException e2) {
					e.addSuppressed( e2 );
					JDBC_MESSAGE_LOGGER.encounteredFailureRollingBackFailedCommit( e2 );
				}
				throw e;
			}
		}

		private void commitRollbackOnly() {
			JDBC_MESSAGE_LOGGER.onCommitMarkedRollbackOnlyRollingBack();
			rollback();
			if ( jpaCompliance.isJpaTransactionComplianceEnabled() ) {
				throw new RollbackException( "Transaction was marked for rollback only" );
			}
		}

		@Override
		public void rollback() {
			try {
				if ( rollbackOnly || jdbcResourceTransaction.getStatus() == TransactionStatus.ACTIVE ) {
					jdbcResourceTransaction.rollback();
					afterCompletionCallback( false );
				}
			}
			finally {
				rollbackOnly = false;
			}
		}

		@Override
		public TransactionStatus getStatus() {
			final var status = jdbcResourceTransaction.getStatus();
			return rollbackOnly && status == TransactionStatus.ACTIVE
					? TransactionStatus.MARKED_ROLLBACK
					: status;
		}

		@Override
		public void markRollbackOnly() {
			if ( getStatus() != TransactionStatus.ROLLED_BACK ) {
				if ( JDBC_LOGGER.isTraceEnabled() ) {
					JDBC_MESSAGE_LOGGER.jdbcTransactionMarkedForRollbackOnly(
							new Exception( "exception just for purpose of providing stack trace" ) );
				}
				rollbackOnly = true;
			}
		}
	}
}
