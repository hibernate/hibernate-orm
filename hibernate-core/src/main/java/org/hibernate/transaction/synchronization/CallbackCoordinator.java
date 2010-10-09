/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.transaction.synchronization;

import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.util.JTAHelper;

/**
 * Manages callbacks from the {@link javax.transaction.Synchronization} registered by Hibernate.
 *
 * @author Steve Ebersole
 */
public class CallbackCoordinator {
	private static final Logger log = LoggerFactory.getLogger( CallbackCoordinator.class );

	private final TransactionFactory.Context ctx;
	private JDBCContext jdbcContext;
	private final Transaction jtaTransaction;
	private final org.hibernate.Transaction hibernateTransaction;

	private BeforeCompletionManagedFlushChecker beforeCompletionManagedFlushChecker;
	private AfterCompletionAction afterCompletionAction;
	private ExceptionMapper exceptionMapper;

	public CallbackCoordinator(
			TransactionFactory.Context ctx,
			JDBCContext jdbcContext,
			Transaction jtaTransaction,
			org.hibernate.Transaction hibernateTransaction) {
		this.ctx = ctx;
		this.jdbcContext = jdbcContext;
		this.jtaTransaction = jtaTransaction;
		this.hibernateTransaction = hibernateTransaction;
		reset();
	}

	public void reset() {
		beforeCompletionManagedFlushChecker = STANDARD_MANAGED_FLUSH_CHECKER;
		exceptionMapper = STANDARD_EXCEPTION_MAPPER;
		afterCompletionAction = STANDARD_AFTER_COMPLETION_ACTION;
	}

	public BeforeCompletionManagedFlushChecker getBeforeCompletionManagedFlushChecker() {
		return beforeCompletionManagedFlushChecker;
	}

	public void setBeforeCompletionManagedFlushChecker(BeforeCompletionManagedFlushChecker beforeCompletionManagedFlushChecker) {
		this.beforeCompletionManagedFlushChecker = beforeCompletionManagedFlushChecker;
	}

	public ExceptionMapper getExceptionMapper() {
		return exceptionMapper;
	}

	public void setExceptionMapper(ExceptionMapper exceptionMapper) {
		this.exceptionMapper = exceptionMapper;
	}

	public AfterCompletionAction getAfterCompletionAction() {
		return afterCompletionAction;
	}

	public void setAfterCompletionAction(AfterCompletionAction afterCompletionAction) {
		this.afterCompletionAction = afterCompletionAction;
	}


	// sync callbacks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void beforeCompletion() {
		log.trace( "transaction before completion callback" );

		boolean flush;
		try {
			flush = beforeCompletionManagedFlushChecker.shouldDoManagedFlush( ctx, jtaTransaction );
		}
		catch ( SystemException se ) {
			setRollbackOnly();
			throw exceptionMapper.mapStatusCheckFailure( "could not determine transaction status in beforeCompletion()", se );
		}

		try {
			if ( flush ) {
				log.trace( "automatically flushing session" );
				ctx.managedFlush();
			}
		}
		catch ( RuntimeException re ) {
			setRollbackOnly();
			throw exceptionMapper.mapManagedFlushFailure( "error during managed flush", re );
		}
		finally {
			jdbcContext.beforeTransactionCompletion( hibernateTransaction );
		}
	}

	private void setRollbackOnly() {
		try {
			jtaTransaction.setRollbackOnly();
		}
		catch ( SystemException se ) {
			// best effort
			log.error( "could not set transaction to rollback only", se );
		}
	}

	public void afterCompletion(int status) {
		log.trace( "transaction after completion callback [status={}]", status );

		try {
			afterCompletionAction.doAction( ctx, status );

			final boolean wasSuccessful = ( status == Status.STATUS_COMMITTED );
			jdbcContext.afterTransactionCompletion( wasSuccessful, hibernateTransaction );
		}
		finally {
			reset();
			jdbcContext.cleanUpJtaSynchronizationCallbackCoordinator();
			if ( ctx.shouldAutoClose() && !ctx.isClosed() ) {
				log.trace( "automatically closing session" );
				ctx.managedClose();
			}
		}
	}

	private static final BeforeCompletionManagedFlushChecker STANDARD_MANAGED_FLUSH_CHECKER = new BeforeCompletionManagedFlushChecker() {
		public boolean shouldDoManagedFlush(TransactionFactory.Context ctx, Transaction jtaTransaction)
				throws SystemException {
			return !ctx.isFlushModeNever() &&
					ctx.isFlushBeforeCompletionEnabled() &&
			        !JTAHelper.isRollback( jtaTransaction.getStatus() );
					//actually, this last test is probably unnecessary, since
					//beforeCompletion() doesn't get called during rollback
		}
	};

	private static final ExceptionMapper STANDARD_EXCEPTION_MAPPER = new ExceptionMapper() {
		public RuntimeException mapStatusCheckFailure(String message, SystemException systemException) {
			log.error( "could not determine transaction status [{}]", systemException.getMessage() );
			return new TransactionException( "could not determine transaction status in beforeCompletion()", systemException );
		}

		public RuntimeException mapManagedFlushFailure(String message, RuntimeException failure) {
			log.error( "Error during managed flush [{}]", failure.getMessage() );
			return failure;
		}
	};

	private static final AfterCompletionAction STANDARD_AFTER_COMPLETION_ACTION = new AfterCompletionAction() {
		public void doAction(TransactionFactory.Context ctx, int status) {
			// nothing to do by default.
		}
	};
}
