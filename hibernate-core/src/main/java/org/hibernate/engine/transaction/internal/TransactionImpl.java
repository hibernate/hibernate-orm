/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.internal;

import jakarta.transaction.Synchronization;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.TransactionException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class TransactionImpl implements TransactionImplementor {

	private final TransactionCoordinator transactionCoordinator;
	private final boolean jpaCompliance;
	private final SharedSessionContractImplementor session;

	private TransactionDriver transactionDriverControl;

	public TransactionImpl(
			TransactionCoordinator transactionCoordinator,
			SharedSessionContractImplementor session) {
		this.transactionCoordinator = transactionCoordinator;
		this.session = session;

		jpaCompliance =
				session.getFactory().getSessionFactoryOptions().getJpaCompliance()
						.isJpaTransactionComplianceEnabled();

		if ( session.isOpen() && transactionCoordinator.isActive() ) {
			transactionDriverControl =
					transactionCoordinator.getTransactionDriverControl();
		}
		else {
			CORE_LOGGER.transactionCreatedOnClosedSession();
		}

		if ( CORE_LOGGER.isDebugEnabled() && jpaCompliance ) {
			CORE_LOGGER.transactionCreatedInJpaCompliantMode();
		}
	}

	@Override
	public void begin() {
		if ( !session.isOpen() ) {
			throw new IllegalStateException( "Cannot begin Transaction on closed Session/EntityManager" );
		}

		if ( transactionDriverControl == null ) {
			transactionDriverControl =
					transactionCoordinator.getTransactionDriverControl();
		}

		if ( isActive() ) {
			if ( jpaCompliance ) {
				throw new IllegalStateException( "Transaction already active (in JPA compliant mode)" );
			}
			else if ( !transactionCoordinator.getTransactionCoordinatorBuilder().isJta() ) {
				throw new IllegalStateException( "Resource-local transaction already active" );
			}
		}
		else {
			CORE_LOGGER.beginningTransaction();
			transactionDriverControl.begin();
		}
	}

	@Override
	public void commit() {
		// allow MARKED_ROLLBACK to propagate through to transactionDriverControl
		if ( !isActive() ) {
			// we have a transaction that is inactive and has not been marked for rollback only
			throw new IllegalStateException( "Transaction not successfully started" );
		}
		else {
			CORE_LOGGER.committingTransaction();
			try {
				internalGetTransactionDriverControl().commit();
			}
			catch (RuntimeException e) {
				throw session.getExceptionConverter().convertCommitException( e );
			}
		}
	}

	public TransactionDriver internalGetTransactionDriverControl() {
		// NOTE here to help be a more descriptive NullPointerException
		if ( transactionDriverControl == null ) {
			throw new IllegalStateException( "Transaction was not properly begun/started" );
		}
		else {
			return transactionDriverControl;
		}
	}

	@Override
	public void rollback() {
		if ( !isActive() && jpaCompliance ) {
			throw new IllegalStateException( "rollback() called on inactive transaction (in JPA compliant mode)" );
		}

		final var status = getStatus();
		if ( status == TransactionStatus.ROLLED_BACK || status == TransactionStatus.NOT_ACTIVE ) {
			// allow rollback() on completed transaction as noop
			CORE_LOGGER.rollbackCalledOnInactiveTransaction();
		}
		else if ( !status.canRollback() ) {
			throw new TransactionException( "Cannot roll back transaction in current status [" + status.name() + "]" );
		}
		else if ( status != TransactionStatus.FAILED_COMMIT || allowFailedCommitToPhysicallyRollback() ) {
			CORE_LOGGER.rollingBackTransaction();
			internalGetTransactionDriverControl().rollback();
		}
	}

	@Override
	public boolean isActive() {
		if ( transactionDriverControl == null ) {
			if ( session.isOpen() ) {
				transactionDriverControl =
						transactionCoordinator.getTransactionDriverControl();
			}
			else {
				return false;
			}
		}
		return transactionDriverControl.isActive();
	}

	@Override
	public TransactionStatus getStatus() {
		if ( transactionDriverControl == null ) {
			if ( session.isOpen() ) {
				transactionDriverControl =
						transactionCoordinator.getTransactionDriverControl();
			}
			else {
				return TransactionStatus.NOT_ACTIVE;
			}
		}
		return transactionDriverControl.getStatus();
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		transactionCoordinator.getLocalSynchronizations()
				.registerSynchronization( synchronization );
	}

	@Override
	public void setTimeout(int seconds) {
		transactionCoordinator.setTimeOut( seconds );
	}

	@Override
	public void setTimeout(@Nullable Integer seconds) {
		transactionCoordinator.setTimeOut( seconds == null ? -1 : seconds );
	}

	@Override
	public @Nullable Integer getTimeout() {
		final int timeout = transactionCoordinator.getTimeOut();
		return timeout == -1 ? null : timeout;
	}

	@Override
	public void markRollbackOnly() {
		// This is the Hibernate-specific API, whereas setRollbackOnly is the
		// JPA-defined API. In our opinion, it's much more user-friendly to
		// always allow the client to indicate that the transaction should
		// not be allowed to commit.
		if ( isActive() ) {
			internalGetTransactionDriverControl().markRollbackOnly();
		}
		// else noop for an inactive transaction
	}

	@Override
	public void setRollbackOnly() {
		if ( !isActive() ) {
			if ( jpaCompliance ) {
				// This is the JPA-defined version of this operation,
				// so we must check that the transaction is active
				throw new IllegalStateException( "setRollbackOnly() called on inactive transaction (in JPA compliant mode)" );
			}
			else {
				// JpaCompliance disables the check, so this method
				// is equivalent to our native markRollbackOnly()
				CORE_LOGGER.setRollbackOnlyCalledOnInactiveTransaction();
			}
		}
		else {
			markRollbackOnly();
		}
	}

	@Override
	public boolean getRollbackOnly() {
		if ( jpaCompliance && !isActive() ) {
			throw new IllegalStateException( "getRollbackOnly() called on inactive transaction (in JPA compliant mode)" );
		}
		else {
			return getStatus() == TransactionStatus.MARKED_ROLLBACK;
		}
	}

	protected boolean allowFailedCommitToPhysicallyRollback() {
		return false;
	}
}
