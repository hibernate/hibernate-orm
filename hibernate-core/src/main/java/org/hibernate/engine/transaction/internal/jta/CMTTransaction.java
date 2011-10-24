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

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.spi.AbstractTransactionImpl;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;

/**
 * Implements a transaction strategy for Container Managed Transaction (CMT) scenarios.  All work is done in
 * the context of the container managed transaction.
 * <p/>
 * The term 'CMT' is potentially misleading; the pertinent point simply being that the transactions are being
 * managed by something other than the Hibernate transaction mechanism.
 * <p/>
 * Additionally, this strategy does *not* attempt to access or use the {@link javax.transaction.UserTransaction} since
 * in the actual case CMT access to the {@link javax.transaction.UserTransaction} is explicitly disallowed.  Instead
 * we use the JTA {@link javax.transaction.Transaction} object obtained from the {@link TransactionManager}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CMTTransaction extends AbstractTransactionImpl {
	private JoinStatus joinStatus = JoinStatus.NOT_JOINED;

	protected CMTTransaction(TransactionCoordinator transactionCoordinator) {
		super( transactionCoordinator );
	}

	protected TransactionManager transactionManager() {
		return jtaPlatform().retrieveTransactionManager();
	}

	private TransactionManager getTransactionManager() {
		return transactionManager();
	}

	@Override
	protected void doBegin() {
		transactionCoordinator().pulse();
	}

	@Override
	protected void afterTransactionBegin() {
		if ( ! transactionCoordinator().isSynchronizationRegistered() ) {
			throw new TransactionException("Could not register synchronization for container transaction");
		}
		transactionCoordinator().sendAfterTransactionBeginNotifications( this );
		transactionCoordinator().getTransactionContext().afterTransactionBegin( this );
	}

	@Override
	protected void beforeTransactionCommit() {
		boolean flush = ! transactionCoordinator().getTransactionContext().isFlushModeNever() &&
				! transactionCoordinator().getTransactionContext().isFlushBeforeCompletionEnabled();
		if ( flush ) {
			// if an exception occurs during flush, user must call rollback()
			transactionCoordinator().getTransactionContext().managedFlush();
		}
	}

	@Override
	protected void doCommit() {
		// nothing to do
	}

	@Override
	protected void beforeTransactionRollBack() {
		// nothing to do
	}

	@Override
	protected void doRollback() {
		markRollbackOnly();
	}

	@Override
	protected void afterTransactionCompletion(int status) {
		// nothing to do
	}

	@Override
	protected void afterAfterCompletion() {
		// nothing to do
	}

	@Override
	public boolean isActive() throws TransactionException {
		return JtaStatusHelper.isActive( getTransactionManager() );
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		return new JtaIsolationDelegate( transactionCoordinator() );
	}

	@Override
	public boolean isInitiator() {
		return false; // cannot be
	}

	@Override
	public void markRollbackOnly() {
		try {
			getTransactionManager().setRollbackOnly();
		}
		catch ( SystemException se ) {
			throw new TransactionException("Could not set transaction to rollback only", se);
		}
	}

	@Override
	public void markForJoin() {
		joinStatus = JoinStatus.MARKED_FOR_JOINED;
	}

	@Override
	public void join() {
		if ( joinStatus != JoinStatus.MARKED_FOR_JOINED ) {
			return;
		}

		if ( JtaStatusHelper.isActive( transactionManager() ) ) {
			// register synchronization if needed
			transactionCoordinator().pulse();
			joinStatus = JoinStatus.JOINED;
		}
		else {
			joinStatus = JoinStatus.NOT_JOINED;
		}
	}

	@Override
	public void resetJoinStatus() {
		joinStatus = JoinStatus.NOT_JOINED;
	}

	boolean isJoinable() {
		return ( joinStatus == JoinStatus.JOINED || joinStatus == JoinStatus.MARKED_FOR_JOINED ) &&
				JtaStatusHelper.isActive( transactionManager() );
	}

	@Override
	public JoinStatus getJoinStatus() {
		return joinStatus;
	}
}
