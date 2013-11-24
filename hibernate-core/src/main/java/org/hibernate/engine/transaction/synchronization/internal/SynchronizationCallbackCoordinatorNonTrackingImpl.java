/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.transaction.synchronization.internal;

import javax.transaction.SystemException;

import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.synchronization.spi.AfterCompletionAction;
import org.hibernate.engine.transaction.synchronization.spi.ExceptionMapper;
import org.hibernate.engine.transaction.synchronization.spi.ManagedFlushChecker;
import org.hibernate.engine.transaction.synchronization.spi.SynchronizationCallbackCoordinator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Manages callbacks from the {@link javax.transaction.Synchronization} registered by Hibernate.
 * 
 * @author Steve Ebersole
 */
public class SynchronizationCallbackCoordinatorNonTrackingImpl implements SynchronizationCallbackCoordinator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger(
			SynchronizationCallbackCoordinatorNonTrackingImpl.class
	);

	private final TransactionCoordinator transactionCoordinator;

	private ManagedFlushChecker managedFlushChecker;
	private AfterCompletionAction afterCompletionAction;
	private ExceptionMapper exceptionMapper;

	public SynchronizationCallbackCoordinatorNonTrackingImpl(TransactionCoordinator transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
		reset();
	}

	public void reset() {
		managedFlushChecker = STANDARD_MANAGED_FLUSH_CHECKER;
		exceptionMapper = STANDARD_EXCEPTION_MAPPER;
		afterCompletionAction = STANDARD_AFTER_COMPLETION_ACTION;
	}

	private TransactionContext transactionContext() {
		return transactionCoordinator.getTransactionContext();
	}

	@Override
	public void setManagedFlushChecker(ManagedFlushChecker managedFlushChecker) {
		this.managedFlushChecker = managedFlushChecker;
	}

	@Override
	public void setExceptionMapper(ExceptionMapper exceptionMapper) {
		this.exceptionMapper = exceptionMapper;
	}

	@Override
	public void setAfterCompletionAction(AfterCompletionAction afterCompletionAction) {
		this.afterCompletionAction = afterCompletionAction;
	}

	// sync callbacks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void beforeCompletion() {
		LOG.trace( "Transaction before completion callback" );

		if ( !transactionCoordinator.isActive() ) {
			return;
		}

		boolean flush;
		try {
			final int status = transactionCoordinator.getTransactionContext().getTransactionEnvironment()
					.getJtaPlatform().getCurrentStatus();
			flush = managedFlushChecker.shouldDoManagedFlush( transactionCoordinator, status );
		}
		catch ( SystemException se ) {
			setRollbackOnly();
			throw exceptionMapper.mapStatusCheckFailure(
					"could not determine transaction status in beforeCompletion()", se );
		}

		try {
			if ( flush ) {
				LOG.trace( "Automatically flushing session" );
				transactionCoordinator.getTransactionContext().managedFlush();
			}
		}
		catch ( RuntimeException re ) {
			setRollbackOnly();
			throw exceptionMapper.mapManagedFlushFailure( "error during managed flush", re );
		}
		finally {
			transactionCoordinator.sendBeforeTransactionCompletionNotifications( null );
			transactionCoordinator.getTransactionContext().beforeTransactionCompletion( null );
		}
	}

	private void setRollbackOnly() {
		transactionCoordinator.setRollbackOnly();
	}

	@Override
	public void afterCompletion(int status) {
		doAfterCompletion( status );
	}

	protected void doAfterCompletion(int status) {
		LOG.tracef( "Starting transaction afterCompletion callback [status=%s]", status );
		if ( !transactionCoordinator.isActive() ) {
			return;
		}

		try {
			afterCompletionAction.doAction( transactionCoordinator, status );
			transactionCoordinator.afterTransaction( null, status );
		}
		finally {
			reset();
			if ( transactionContext().shouldAutoClose() && !transactionContext().isClosed() ) {
				LOG.trace( "Automatically closing session" );
				transactionContext().managedClose();
			}
		}
	}

	@Override
	public void synchronizationRegistered() {
	}

	@Override
	public void processAnyDelayedAfterCompletion() {
	}

	private static final ManagedFlushChecker STANDARD_MANAGED_FLUSH_CHECKER = new ManagedFlushChecker() {
		@Override
		public boolean shouldDoManagedFlush(TransactionCoordinator coordinator, int jtaStatus) {
			return !coordinator.getTransactionContext().isClosed()
					&& !coordinator.getTransactionContext().isFlushModeNever()
					&& coordinator.getTransactionContext().isFlushBeforeCompletionEnabled()
					&& !JtaStatusHelper.isRollback( jtaStatus );
		}
	};

	private static final ExceptionMapper STANDARD_EXCEPTION_MAPPER = new ExceptionMapper() {
		@Override
		public RuntimeException mapStatusCheckFailure(String message, SystemException systemException) {
			LOG.error( LOG.unableToDetermineTransactionStatus(), systemException );
			return new TransactionException( "could not determine transaction status in beforeCompletion()",
					systemException );
		}

		@Override
		public RuntimeException mapManagedFlushFailure(String message, RuntimeException failure) {
			LOG.unableToPerformManagedFlush( failure.getMessage() );
			return failure;
		}
	};

	private static final AfterCompletionAction STANDARD_AFTER_COMPLETION_ACTION = new AfterCompletionAction() {
		@Override
		public void doAction(TransactionCoordinator transactionCoordinator, int status) {
			// nothing to do by default.
		}
	};
}
