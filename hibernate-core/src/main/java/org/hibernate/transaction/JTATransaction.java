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
package org.hibernate.transaction;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.engine.jdbc.spi.JDBCContext;
import org.hibernate.util.JTAHelper;

/**
 * {@link Transaction} implementation based on transaction management through
 * a JTA {@link UserTransaction}.  Similar to {@link CMTTransaction}, except
 * here we are actually managing the transactions through the Hibernate
 * transaction mechanism.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Les Hazlewood
 */
public class JTATransaction implements Transaction {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                JTATransaction.class.getPackage().getName());

	private final JDBCContext jdbcContext;
	private final TransactionFactory.Context transactionContext;

	private UserTransaction userTransaction;
	private boolean newTransaction;
	private boolean begun;
	private boolean commitFailed;
	private boolean commitSucceeded;
	private boolean callback;

	public JTATransaction(
			UserTransaction userTransaction,
			JDBCContext jdbcContext,
			TransactionFactory.Context transactionContext) {
		this.jdbcContext = jdbcContext;
		this.transactionContext = transactionContext;
		this.userTransaction = userTransaction;
	}

	/**
	 * {@inheritDoc}
	 */
	public void begin() throws HibernateException {
		if ( begun ) {
			return;
		}
		if ( commitFailed ) {
			throw new TransactionException( "cannot re-start transaction after failed commit" );
		}

        LOG.begin();

		try {
			newTransaction = userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION;
			if ( newTransaction ) {
				userTransaction.begin();
				LOG.beganNewJtaTransaction();
			}
		}
		catch ( Exception e ) {
            LOG.error(LOG.unableToBeginJtaTransaction(), e);
            throw new TransactionException(LOG.unableToBeginJtaTransaction(), e);
		}

		/*if (newTransaction) {
			// don't need a synchronization since we are committing
			// or rolling back the transaction ourselves - assuming
			// that we do no work in beforeTransactionCompletion()
			synchronization = false;
		}*/

		boolean synchronization = jdbcContext.registerSynchronizationIfPossible();

        if (!newTransaction && !synchronization) LOG.managerLookupClassShouldBeSet();

		if ( !synchronization ) {
			//if we could not register a synchronization,
			//do the before/after completion callbacks
			//ourself (but we need to let jdbcContext
			//know that this is what we are going to
			//do, so it doesn't keep trying to register
			//synchronizations)
			callback = jdbcContext.registerCallbackIfNecessary();
		}

		begun = true;
		commitSucceeded = false;

		jdbcContext.afterTransactionBegin( this );
	}

	/**
	 * {@inheritDoc}
	 */
	public void commit() throws HibernateException {
		if ( !begun ) {
			throw new TransactionException( "Transaction not successfully started" );
		}

        LOG.commit();

		boolean flush = !transactionContext.isFlushModeNever()
				&& ( callback || !transactionContext.isFlushBeforeCompletionEnabled() );

		if ( flush ) {
			transactionContext.managedFlush(); //if an exception occurs during flush, user must call rollback()
		}

		if ( callback && newTransaction ) {
			jdbcContext.beforeTransactionCompletion( this );
		}

		closeIfRequired();

		if ( newTransaction ) {
			try {
				userTransaction.commit();
				commitSucceeded = true;
                LOG.commitedJtaUserTransaction();
			}
			catch ( Exception e ) {
				commitFailed = true; // so the transaction is already rolled back, by JTA spec
                LOG.error(LOG.unableToCommitJta(), e);
                throw new TransactionException(LOG.unableToCommitJta(), e);
			}
			finally {
				afterCommitRollback();
			}
		}
		else {
			// this one only really needed for badly-behaved applications!
			// (if the TransactionManager has a Sychronization registered,
			// its a noop)
			// (actually we do need it for downgrading locks)
			afterCommitRollback();
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public void rollback() throws HibernateException {
		if ( !begun && !commitFailed ) {
			throw new TransactionException( "Transaction not successfully started" );
		}

        LOG.rollback();

		try {
			closeIfRequired();
		}
		catch ( Exception e ) {
			// swallow it, and continue to roll back JTA transaction
            LOG.error(LOG.unableToCloseSessionDuringRollback(), e);
		}

		try {
			if ( newTransaction ) {
				if ( !commitFailed ) {
					userTransaction.rollback();
                    LOG.rolledBackJtaUserTransaction();
				}
			}
			else {
				userTransaction.setRollbackOnly();
                LOG.jtaUserTransactionSetToRollbackOnly();
			}
		}
		catch ( Exception e ) {
            LOG.error(LOG.unableToRollbackJta(), e);
            throw new TransactionException(LOG.unableToRollbackJta(), e);
		}
		finally {
			afterCommitRollback();
		}
	}

	private static final int NULL = Integer.MIN_VALUE;

	private void afterCommitRollback() throws TransactionException {

		begun = false;
		// this method is a noop if there is a Synchronization!
		if ( callback ) {
            if (!newTransaction) LOG.managerLookupClassShouldBeSet();
			int status = NULL;
			try {
				status = userTransaction.getStatus();
			}
			catch ( Exception e ) {
                LOG.error(LOG.unableToDetermineTransactionStatusAfterCommit(), e);
                throw new TransactionException(LOG.unableToDetermineTransactionStatusAfterCommit(), e);
			}
			finally {
				jdbcContext.afterTransactionCompletion( status == Status.STATUS_COMMITTED, this );
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean wasRolledBack() throws TransactionException {
		final int status;
		try {
			status = userTransaction.getStatus();
		}
		catch ( SystemException se ) {
            LOG.error(LOG.unableToDetermineTransactionStatus(), se);
            throw new TransactionException(LOG.unableToDetermineTransactionStatus(), se);
		}
        if (status == Status.STATUS_UNKNOWN) throw new TransactionException(LOG.unableToDetermineTransactionStatus());
        return JTAHelper.isRollback(status);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean wasCommitted() throws TransactionException {
		final int status;
		try {
			status = userTransaction.getStatus();
		}
		catch ( SystemException se ) {
            LOG.error(LOG.unableToDetermineTransactionStatus(), se);
            throw new TransactionException(LOG.unableToDetermineTransactionStatus(), se);
		}
        if (status == Status.STATUS_UNKNOWN) throw new TransactionException(LOG.unableToDetermineTransactionStatus());
        return status == Status.STATUS_COMMITTED;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isActive() throws TransactionException {
		if ( !begun || commitFailed || commitSucceeded ) {
			return false;
		}

		final int status;
		try {
			status = userTransaction.getStatus();
		}
		catch ( SystemException se ) {
            LOG.error(LOG.unableToDetermineTransactionStatus(), se);
			throw new TransactionException( "Could not determine transaction status: ", se );
		}
        if (status == Status.STATUS_UNKNOWN) throw new TransactionException("Could not determine transaction status");
        return status == Status.STATUS_ACTIVE;
	}

	/**
	 * {@inheritDoc}
	 */
	public void registerSynchronization(Synchronization sync) throws HibernateException {
		if ( getTransactionManager() == null ) {
			throw new IllegalStateException( "JTA TransactionManager not available" );
		}
		else {
			try {
				getTransactionManager().getTransaction().registerSynchronization( sync );
			}
			catch ( Exception e ) {
				throw new TransactionException( "could not register synchronization", e );
			}
		}
	}

	/**
	 * Getter for property 'transactionManager'.
	 *
	 * @return Value for property 'transactionManager'.
	 */
	private TransactionManager getTransactionManager() {
		return transactionContext.getFactory().getTransactionManager();
	}

	private void closeIfRequired() throws HibernateException {
		boolean close = callback &&
				transactionContext.shouldAutoClose() &&
				!transactionContext.isClosed();
		if ( close ) {
			transactionContext.managedClose();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTimeout(int seconds) {
		try {
			userTransaction.setTransactionTimeout( seconds );
		}
		catch ( SystemException se ) {
			throw new TransactionException( "could not set transaction timeout", se );
		}
	}

	/**
	 * Getter for property 'userTransaction'.
	 *
	 * @return Value for property 'userTransaction'.
	 */
	protected UserTransaction getUserTransaction() {
		return userTransaction;
	}
}
