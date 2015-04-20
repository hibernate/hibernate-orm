/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) {DATE}, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.transaction.internal;

import javax.transaction.Synchronization;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import static org.hibernate.resource.transaction.TransactionCoordinator.LocalInflow;

/**
 * @author Andrea Boriero
 */
public class TransactionImpl implements TransactionImplementor {
	private static final Logger LOG = CoreLogging.logger( TransactionImpl.class );

	private final TransactionCoordinator transactionCoordinator;
	private final LocalInflow transactionDriverControl;

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
			throw new TransactionException( "nested transactions not supported" );
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
		catch (Exception e) {
			throw new TransactionException( "commit failed", e );
		}
		finally {
			invalidate();
		}
	}

	@Override
	public void rollback() {
		TransactionStatus status = transactionDriverControl.getStatus();
		if ( status != TransactionStatus.ACTIVE && status != TransactionStatus.FAILED_COMMIT ) {
			throw new TransactionException( "Transaction not successfully started" );
		}

		LOG.debug( "rolling back" );
		if ( status != TransactionStatus.FAILED_COMMIT || allowFailedCommitToPhysicallyRollback() ) {
			try {
				this.transactionDriverControl.rollback();
			}
			catch (Exception e) {
				throw new TransactionException( "rollback failed", e );
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

	@Override
	public IsolationDelegate createIsolationDelegate() {
		return transactionCoordinator.createIsolationDelegate();
	}
}
