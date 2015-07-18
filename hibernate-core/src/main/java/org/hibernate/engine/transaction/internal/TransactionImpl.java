/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.internal;

import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.jboss.logging.Logger;

import static org.hibernate.resource.transaction.TransactionCoordinator.TransactionDriver;

/**
 * @author Andrea Boriero
 */
public class TransactionImpl implements Transaction {
	private static final Logger LOG = CoreLogging.logger( TransactionImpl.class );

	private final TransactionCoordinator transactionCoordinator;
	private final TransactionDriver transactionDriverControl;

	private boolean valid = true;

	public TransactionImpl(TransactionCoordinator transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
		this.transactionDriverControl = transactionCoordinator.getTransactionDriverControl();
	}

	@Override
	public void begin() {
		TransactionStatus status = transactionDriverControl.getStatus();

		if ( !valid ) {
			throw new TransactionException( "Transaction instance is no longer valid" );
		}
		if ( status == TransactionStatus.ACTIVE ) {
//			throw new TransactionException( "nested transactions not supported" );
			return;
		}

		LOG.debug( "begin" );
		this.transactionDriverControl.begin();
	}

	@Override
	public void commit() {
		TransactionStatus status = transactionDriverControl.getStatus();
		if ( status != TransactionStatus.ACTIVE ) {
			throw new TransactionException( "Transaction not successfully started" );
		}

		LOG.debug( "committing" );

		try {
			this.transactionDriverControl.commit();
		}
		finally {
			invalidate();
		}
	}

	@Override
	public void rollback() {
		TransactionStatus status = transactionDriverControl.getStatus();
		if ( !status.canRollback() ) {
			throw new TransactionException( "Cannot rollback transaction in current status [" + status.name() + "]" );
		}

		LOG.debug( "rolling back" );
		if ( status != TransactionStatus.FAILED_COMMIT || allowFailedCommitToPhysicallyRollback() ) {
			try {
				this.transactionDriverControl.rollback();
			}
			finally {
				invalidate();
			}
		}
	}

	@Override
	public TransactionStatus getStatus() {
		return transactionDriverControl.getStatus();
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) throws HibernateException {
		this.transactionCoordinator.getLocalSynchronizations().registerSynchronization( synchronization );
	}

	@Override
	public void setTimeout(int seconds) {
		this.transactionCoordinator.setTimeOut( seconds );
	}

	@Override
	public int getTimeout() {
		return this.transactionCoordinator.getTimeOut();
	}

	@Override
	public void markRollbackOnly() {
		transactionDriverControl.markRollbackOnly();
	}

	public void invalidate() {
		valid = false;
	}

	protected boolean allowFailedCommitToPhysicallyRollback() {
		return false;
	}
}
