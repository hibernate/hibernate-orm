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
package org.hibernate.test.cache.jbc2.functional.util;

import java.util.Hashtable;

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
 * Variant of SimpleJtaTransactionManagerImpl that doesn't use a VM-singleton,
 * but rather a set of impls keyed by a node id.
 *
 * @author Brian Stansberry
 */
public class DualNodeJtaTransactionManagerImpl implements TransactionManager {
	private static final Hashtable INSTANCES = new Hashtable();

	private DualNodeJtaTransactionImpl currentTransaction;

	public synchronized static DualNodeJtaTransactionManagerImpl getInstance(String nodeId) {
	    DualNodeJtaTransactionManagerImpl tm = (DualNodeJtaTransactionManagerImpl) INSTANCES.get(nodeId);
	    if (tm == null) {
	        tm = new DualNodeJtaTransactionManagerImpl();
	        INSTANCES.put(nodeId, tm);
	    }
		return tm;
	}

	public int getStatus() throws SystemException {
		return currentTransaction == null ? Status.STATUS_NO_TRANSACTION : currentTransaction.getStatus();
	}

	public Transaction getTransaction() throws SystemException {
		return currentTransaction;
	}

	public DualNodeJtaTransactionImpl getCurrentTransaction() {
		return currentTransaction;
	}

	public void begin() throws NotSupportedException, SystemException {
		currentTransaction = new DualNodeJtaTransactionImpl( this );
	}

	public Transaction suspend() throws SystemException {
	   DualNodeJtaTransactionImpl suspended = currentTransaction;
		currentTransaction = null;
		return suspended;
	}

	public void resume(Transaction transaction)
			throws InvalidTransactionException, IllegalStateException, SystemException {
		currentTransaction = ( DualNodeJtaTransactionImpl ) transaction;
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

	void endCurrent(DualNodeJtaTransactionImpl transaction) {
		if ( transaction == currentTransaction ) {
			currentTransaction = null;
		}
	}
}
