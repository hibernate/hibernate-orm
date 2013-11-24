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
package org.hibernate.engine.transaction.spi;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.internal.CoreLogging;

import org.jboss.logging.Logger;

/**
 * Abstract support for creating {@link TransactionImplementor transaction} implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTransactionImpl implements TransactionImplementor {
	private static final Logger LOG = CoreLogging.logger( AbstractTransactionImpl.class );

	private final TransactionCoordinator transactionCoordinator;

	private boolean valid = true;

	private LocalStatus localStatus = LocalStatus.NOT_ACTIVE;
	private int timeout = -1;

	protected AbstractTransactionImpl(TransactionCoordinator transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
	}

	@Override
	public void invalidate() {
		valid = false;
	}

	/**
	 * Perform the actual steps of beginning a transaction according to the strategy.
	 *
	 * @throws org.hibernate.TransactionException Indicates a problem beginning the transaction
	 */
	protected abstract void doBegin();

	/**
	 * Perform the actual steps of committing a transaction according to the strategy.
	 *
	 * @throws org.hibernate.TransactionException Indicates a problem committing the transaction
	 */
	protected abstract void doCommit();

	/**
	 * Perform the actual steps of rolling back a transaction according to the strategy.
	 *
	 * @throws org.hibernate.TransactionException Indicates a problem rolling back the transaction
	 */
	protected abstract void doRollback();

	protected abstract void afterTransactionBegin();

	protected abstract void beforeTransactionCommit();

	protected abstract void beforeTransactionRollBack();

	protected abstract void afterTransactionCompletion(int status);

	protected abstract void afterAfterCompletion();

	/**
	 * Provide subclasses with access to the transaction coordinator.
	 *
	 * @return This transaction's context.
	 */
	protected TransactionCoordinator transactionCoordinator() {
		return transactionCoordinator;
	}

	/**
	 * Provide subclasses with convenient access to the configured {@link JtaPlatform}
	 *
	 * @return The {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
	 */
	protected JtaPlatform jtaPlatform() {
		return transactionCoordinator().getTransactionContext().getTransactionEnvironment().getJtaPlatform();
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		transactionCoordinator().getSynchronizationRegistry().registerSynchronization( synchronization );
	}

	@Override
	public LocalStatus getLocalStatus() {
		return localStatus;
	}

	@Override
	public boolean isActive() {
		return localStatus == LocalStatus.ACTIVE && doExtendedActiveCheck();
	}

	@Override
	public boolean isParticipating() {
		return getJoinStatus() == JoinStatus.JOINED && isActive();
	}

	@Override
	public boolean wasCommitted() {
		return localStatus == LocalStatus.COMMITTED;
	}

	@Override
	public boolean wasRolledBack() throws HibernateException {
		return localStatus == LocalStatus.ROLLED_BACK;
	}

	/**
	 * Active has been checked against local state.  Perform any needed checks against resource transactions.
	 *
	 * @return {@code true} if the extended active check checks out as well; false otherwise.
	 */
	protected boolean doExtendedActiveCheck() {
		return true;
	}

	@Override
	public void begin() throws HibernateException {
		if ( !valid ) {
			throw new TransactionException( "Transaction instance is no longer valid" );
		}
		if ( localStatus == LocalStatus.ACTIVE ) {
			throw new TransactionException( "nested transactions not supported" );
		}
		if ( localStatus != LocalStatus.NOT_ACTIVE ) {
			throw new TransactionException( "reuse of Transaction instances not supported" );
		}

		LOG.debug( "begin" );

		doBegin();

		localStatus = LocalStatus.ACTIVE;

		afterTransactionBegin();
	}

	@Override
	public void commit() throws HibernateException {
		if ( localStatus != LocalStatus.ACTIVE ) {
			throw new TransactionException( "Transaction not successfully started" );
		}

		LOG.debug( "committing" );

		beforeTransactionCommit();

		try {
			doCommit();
			localStatus = LocalStatus.COMMITTED;
			afterTransactionCompletion( Status.STATUS_COMMITTED );
		}
		catch (Exception e) {
			localStatus = LocalStatus.FAILED_COMMIT;
			afterTransactionCompletion( Status.STATUS_UNKNOWN );
			throw new TransactionException( "commit failed", e );
		}
		finally {
			invalidate();
			afterAfterCompletion();
		}
	}

	protected boolean allowFailedCommitToPhysicallyRollback() {
		return false;
	}

	@Override
	public void rollback() throws HibernateException {
		if ( localStatus != LocalStatus.ACTIVE && localStatus != LocalStatus.FAILED_COMMIT ) {
			throw new TransactionException( "Transaction not successfully started" );
		}

		LOG.debug( "rolling back" );

		beforeTransactionRollBack();

		if ( localStatus != LocalStatus.FAILED_COMMIT || allowFailedCommitToPhysicallyRollback() ) {
			try {
				doRollback();
				localStatus = LocalStatus.ROLLED_BACK;
				afterTransactionCompletion( Status.STATUS_ROLLEDBACK );
			}
			catch (Exception e) {
				afterTransactionCompletion( Status.STATUS_UNKNOWN );
				throw new TransactionException( "rollback failed", e );
			}
			finally {
				invalidate();
				afterAfterCompletion();
			}
		}

	}

	@Override
	public void setTimeout(int seconds) {
		timeout = seconds;
	}

	@Override
	public int getTimeout() {
		return timeout;
	}

	@Override
	public void markForJoin() {
		// generally speaking this is no-op
	}

	@Override
	public void join() {
		// generally speaking this is no-op
	}

	@Override
	public void resetJoinStatus() {
		// generally speaking this is no-op
	}
}
