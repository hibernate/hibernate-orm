/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.internal;

import jakarta.transaction.Synchronization;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.internal.AbstractSharedSessionContract;
import org.hibernate.internal.CoreLogging;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.jboss.logging.Logger;

import static org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class TransactionImpl implements TransactionImplementor {
	private static final Logger LOG = CoreLogging.logger( TransactionImpl.class );

	private final TransactionCoordinator transactionCoordinator;
	private final boolean jpaCompliance;
	private final AbstractSharedSessionContract session;

	private TransactionDriver transactionDriverControl;

	public TransactionImpl(
			TransactionCoordinator transactionCoordinator,
			AbstractSharedSessionContract session) {
		this.transactionCoordinator = transactionCoordinator;
		this.jpaCompliance =
				session.getFactory().getSessionFactoryOptions().getJpaCompliance()
						.isJpaTransactionComplianceEnabled();
		this.session = session;

		if ( session.isOpen() && transactionCoordinator.isActive() ) {
			this.transactionDriverControl = transactionCoordinator.getTransactionDriverControl();
		}
		else {
			LOG.debug( "TransactionImpl created on closed Session/EntityManager" );
		}

		if ( LOG.isDebugEnabled() && jpaCompliance ) {
			LOG.debugf( "TransactionImpl created in JPA compliant mode" );
		}
	}

	@Override
	public void begin() {
		if ( !session.isOpen() ) {
			throw new IllegalStateException( "Cannot begin Transaction on closed Session/EntityManager" );
		}

		if ( transactionDriverControl == null ) {
			transactionDriverControl = transactionCoordinator.getTransactionDriverControl();
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
			LOG.debug( "begin transaction" );
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
			LOG.debug( "committing transaction" );
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

		final TransactionStatus status = getStatus();
		if ( status == TransactionStatus.ROLLED_BACK || status == TransactionStatus.NOT_ACTIVE ) {
			// allow rollback() on completed transaction as noop
			LOG.debug( "rollback() called on an inactive transaction" );
		}
		else if ( !status.canRollback() ) {
			throw new TransactionException( "Cannot roll back transaction in current status [" + status.name() + "]" );
		}
		else if ( status != TransactionStatus.FAILED_COMMIT || allowFailedCommitToPhysicallyRollback() ) {
			LOG.debug( "rolling back transaction" );
			internalGetTransactionDriverControl().rollback();
		}
	}

	@Override
	public boolean isActive() {
		if ( transactionDriverControl == null ) {
			if ( session.isOpen() ) {
				transactionDriverControl = transactionCoordinator.getTransactionDriverControl();
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
				transactionDriverControl = transactionCoordinator.getTransactionDriverControl();
			}
			else {
				return TransactionStatus.NOT_ACTIVE;
			}
		}
		return transactionDriverControl.getStatus();
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) throws HibernateException {
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( synchronization );
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
		// this is the Hibernate-specific API, whereas setRollbackOnly is the
		// JPA-defined API. In our opinion it is much more user-friendly to
		// always allow user/integration to indicate that the transaction
		// should not be allowed to commit.
		//
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
				// is equivalent our native markRollbackOnly()
				LOG.debug( "setRollbackOnly() called on a inactive transaction" );
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
