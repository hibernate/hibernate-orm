/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.transaction.internal.jta;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.spi.AbstractTransactionImpl;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.LocalStatus;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Implements a transaction strategy based on transaction management through a JTA {@link UserTransaction}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Les Hazlewood
 */
public class JtaTransaction extends AbstractTransactionImpl {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JtaTransaction.class );

	private UserTransaction userTransaction;

	private boolean isInitiator;
	private boolean isDriver;

	protected JtaTransaction(TransactionCoordinator transactionCoordinator) {
		super( transactionCoordinator );
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public UserTransaction getUserTransaction() {
		return userTransaction;
	}

	@Override
	protected void doBegin() {
		LOG.debug( "begin" );

		userTransaction = locateUserTransaction();

		try {
			if ( userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION ) {
				userTransaction.begin();
				isInitiator = true;
				LOG.debug( "Began a new JTA transaction" );
			}
		}
		catch ( Exception e ) {
			throw new TransactionException( "JTA transaction begin failed", e );
		}
	}

	private UserTransaction locateUserTransaction() {
		final UserTransaction userTransaction = jtaPlatform().retrieveUserTransaction();
		if ( userTransaction == null ) {
			throw new TransactionException( "Unable to locate JTA UserTransaction" );
		}
		return userTransaction;
	}

	@Override
	protected void afterTransactionBegin() {
		transactionCoordinator().pulse();

		if ( !transactionCoordinator().isSynchronizationRegistered() ) {
			isDriver = transactionCoordinator().takeOwnership();
		}

		applyTimeout();
		transactionCoordinator().sendAfterTransactionBeginNotifications( this );
		transactionCoordinator().getTransactionContext().afterTransactionBegin( this );
	}

	private void applyTimeout() {
		if ( getTimeout() > 0 ) {
			if ( userTransaction != null ) {
				try {
					userTransaction.setTransactionTimeout( getTimeout() );
				}
				catch ( SystemException e ) {
					throw new TransactionException( "Unable to apply requested transaction timeout", e );
				}
			}
			else {
				LOG.debug( "Unable to apply requested transaction timeout; no UserTransaction.  Will try later" );
			}
		}
	}

	@Override
	protected void beforeTransactionCommit() {
		transactionCoordinator().sendBeforeTransactionCompletionNotifications( this );

		final boolean flush = ! transactionCoordinator().getTransactionContext().isFlushModeNever() &&
				( isDriver || ! transactionCoordinator().getTransactionContext().isFlushBeforeCompletionEnabled() );

		if ( flush ) {
			// if an exception occurs during flush, user must call rollback()
			transactionCoordinator().getTransactionContext().managedFlush();
		}

		if ( isDriver && isInitiator ) {
			transactionCoordinator().getTransactionContext().beforeTransactionCompletion( this );
		}

		closeIfRequired();
	}

	private void closeIfRequired() throws HibernateException {
		final boolean close = isDriver &&
				transactionCoordinator().getTransactionContext().shouldAutoClose() &&
				! transactionCoordinator().getTransactionContext().isClosed();
		if ( close ) {
			transactionCoordinator().getTransactionContext().managedClose();
		}
	}

	@Override
	protected void doCommit() {
		try {
			if ( isInitiator ) {
				userTransaction.commit();
				LOG.debug( "Committed JTA UserTransaction" );
			}
		}
		catch ( Exception e ) {
			throw new TransactionException( "JTA commit failed: ", e );
		}
	}

	@Override
	protected void afterTransactionCompletion(int status) {
		// nothing to do
	}

	@Override
	protected void afterAfterCompletion() {
		// this method is a noop if there is a Synchronization!
		try {
			if ( isDriver ) {
				if ( !isInitiator ) {
					LOG.setManagerLookupClass();
				}
				try {
					transactionCoordinator().afterTransaction( this, userTransaction.getStatus() );
				}
				catch (SystemException e) {
					throw new TransactionException( "Unable to determine UserTransaction status", e );
				}
			}
		}
		finally {
			isInitiator = false;
		}
	}

	@Override
	protected void beforeTransactionRollBack() {
		// nothing to do
	}

	@Override
	protected void doRollback() {
		try {
			if ( isInitiator ) {
				// failed commits automatically rollback the transaction per JTA spec
				if ( getLocalStatus() != LocalStatus.FAILED_COMMIT  ) {
					userTransaction.rollback();
					LOG.debug( "Rolled back JTA UserTransaction" );
				}
			}
			else {
				markRollbackOnly();
			}
		}
		catch ( Exception e ) {
			throw new TransactionException( "JTA rollback failed", e );
		}
	}

	@Override
	public void markRollbackOnly() {
		LOG.trace( "Marking transaction for rollback only" );
		try {
			if ( userTransaction == null ) {
				userTransaction = locateUserTransaction();
			}
			userTransaction.setRollbackOnly();
			LOG.debug( "set JTA UserTransaction to rollback only" );
		}
		catch (SystemException e) {
			LOG.debug( "Unable to mark transaction for rollback only", e );
		}
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		return new JtaIsolationDelegate( transactionCoordinator() );
	}

	@Override
	public boolean isInitiator() {
		return isInitiator;
	}

	@Override
	public boolean isActive() throws HibernateException {
		if ( getLocalStatus() != LocalStatus.ACTIVE ) {
			return false;
		}

		final int status;
		try {
			status = userTransaction.getStatus();
		}
		catch ( SystemException se ) {
			throw new TransactionException( "Could not determine transaction status: ", se );
		}
		return JtaStatusHelper.isActive( status );
	}

	@Override
	public void setTimeout(int seconds) {
		super.setTimeout( seconds );
		applyTimeout();
	}

	@Override
	public void join() {
	}

	@Override
	public void resetJoinStatus() {
	}

	@Override
	public JoinStatus getJoinStatus() {
		// if we already have the UserTransaction cached locally, use it to avoid JNDI look ups
		if ( this.userTransaction != null ) {
			return JtaStatusHelper.isActive( this.userTransaction ) ? JoinStatus.JOINED : JoinStatus.NOT_JOINED;
		}

		// Otherwise, try to use the TransactionManager since it is generally cached
		TransactionManager transactionManager = jtaPlatform().retrieveTransactionManager();
		if ( transactionManager != null ) {
			return JtaStatusHelper.isActive( transactionManager ) ? JoinStatus.JOINED : JoinStatus.NOT_JOINED;
		}

		// Finally, look up the UserTransaction
		UserTransaction userTransaction = jtaPlatform().retrieveUserTransaction();
		return userTransaction != null && JtaStatusHelper.isActive( userTransaction )
				? JoinStatus.JOINED
				: JoinStatus.NOT_JOINED;
	}
}
