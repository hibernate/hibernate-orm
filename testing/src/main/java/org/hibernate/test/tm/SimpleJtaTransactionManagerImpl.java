/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 */
package org.hibernate.test.tm;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * TransactionManager implementation specifically designed for use in test suite and simple usage
 * scenarios.  For example, it assumes that there is only ever a single transaction active at a
 * given time.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class SimpleJtaTransactionManagerImpl implements TransactionManager {
	private static final SimpleJtaTransactionManagerImpl INSTANCE = new SimpleJtaTransactionManagerImpl();

	private SimpleJtaTransactionImpl currentTransaction;

	public static SimpleJtaTransactionManagerImpl getInstance() {
		return INSTANCE;
	}

	public int getStatus() throws SystemException {
		return currentTransaction == null ? Status.STATUS_NO_TRANSACTION : currentTransaction.getStatus();
	}

	public Transaction getTransaction() throws SystemException {
		return currentTransaction;
	}

	public SimpleJtaTransactionImpl getCurrentTransaction() {
		return currentTransaction;
	}

	public void begin() throws NotSupportedException, SystemException {
		currentTransaction = new SimpleJtaTransactionImpl( this );
	}

	public Transaction suspend() throws SystemException {
		SimpleJtaTransactionImpl suspended = currentTransaction;
		currentTransaction = null;
		return suspended;
	}

	public void resume(Transaction transaction)
			throws InvalidTransactionException, IllegalStateException, SystemException {
		currentTransaction = ( SimpleJtaTransactionImpl ) transaction;
	}

	public void commit()
			throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
		if ( currentTransaction == null ) {
			throw new IllegalStateException( "no current transaction to commit" );
		}
		currentTransaction.commit();
	}

	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		if ( currentTransaction == null ) {
			throw new IllegalStateException( "no current transaction" );
		}
		currentTransaction.rollback();
	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		if ( currentTransaction == null ) {
			throw new IllegalStateException( "no current transaction" );
		}
		currentTransaction.setRollbackOnly();
	}

	public void setTransactionTimeout(int i) throws SystemException {
	}

	void endCurrent(SimpleJtaTransactionImpl transaction) {
		if ( transaction == currentTransaction ) {
			currentTransaction = null;
		}
	}
}
