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
package org.hibernate.engine.transaction.synchronization.internal;

import javax.transaction.SystemException;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.synchronization.spi.AfterCompletionAction;
import org.hibernate.engine.transaction.synchronization.spi.ExceptionMapper;
import org.hibernate.engine.transaction.synchronization.spi.ManagedFlushChecker;
import org.hibernate.engine.transaction.synchronization.spi.SynchronizationCallbackCoordinator;
import org.hibernate.internal.CoreMessageLogger;
import org.jboss.logging.Logger;

/**
 * Manages callbacks from the {@link javax.transaction.Synchronization} registered by Hibernate.
 * 
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class SynchronizationCallbackCoordinatorImpl implements SynchronizationCallbackCoordinator {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class,
			SynchronizationCallbackCoordinatorImpl.class.getName() );

	private final TransactionCoordinator transactionCoordinator;
	private final Settings settings;

	private ManagedFlushChecker managedFlushChecker;
	private AfterCompletionAction afterCompletionAction;
	private ExceptionMapper exceptionMapper;

	private volatile long registrationThreadId;
	private final int NO_STATUS = -1;
	private volatile int delayedCompletionHandlingStatus;

	public SynchronizationCallbackCoordinatorImpl(TransactionCoordinator transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
		this.settings = transactionCoordinator.getTransactionContext()
				.getTransactionEnvironment().getSessionFactory().getSettings();
		reset();
		pulse();
	}

	public void reset() {
		managedFlushChecker = STANDARD_MANAGED_FLUSH_CHECKER;
		exceptionMapper = STANDARD_EXCEPTION_MAPPER;
		afterCompletionAction = STANDARD_AFTER_COMPLETION_ACTION;
		delayedCompletionHandlingStatus = NO_STATUS;
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

	public void beforeCompletion() {
		LOG.trace( "Transaction before completion callback" );

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

	public void afterCompletion(int status) {
		if ( settings.isJtaTrackByThread() && !isRegistrationThread()
				&& JtaStatusHelper.isRollback( status ) ) {
			// The transaction was rolled back by another thread -- not the
			// original application. Examples of this include a JTA transaction
			// timeout getting cleaned up by a reaper thread. If this happens,
			// afterCompletion must be handled by the original thread in order
			// to prevent non-threadsafe session use. Set the flag here and
			// check for it in SessionImpl. See HHH-7910.
			LOG.warnv( "Transaction afterCompletion called by a background thread! Delaying action until the original thread can handle it. [status={0}]", status );
			delayedCompletionHandlingStatus = status;
		}
		else {
			doAfterCompletion( status );
		}
	}
	
	public void pulse() {
		if ( settings.isJtaTrackByThread() ) {
			registrationThreadId = Thread.currentThread().getId();
		}
	}

	public void delayedAfterCompletion() {
		if ( delayedCompletionHandlingStatus != NO_STATUS ) {
			doAfterCompletion( delayedCompletionHandlingStatus );
			delayedCompletionHandlingStatus = NO_STATUS;
			throw new HibernateException("Transaction was rolled back in a different thread!");
		}
	}

	private void doAfterCompletion(int status) {
		LOG.tracev( "Transaction afterCompletion callback [status={0}]", status );

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

	private boolean isRegistrationThread() {
		return Thread.currentThread().getId() == registrationThreadId;
	}

	private TransactionContext transactionContext() {
		return transactionCoordinator.getTransactionContext();
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
		public RuntimeException mapStatusCheckFailure(String message, SystemException systemException) {
			LOG.error( LOG.unableToDetermineTransactionStatus(), systemException );
			return new TransactionException( "could not determine transaction status in beforeCompletion()",
					systemException );
		}

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
