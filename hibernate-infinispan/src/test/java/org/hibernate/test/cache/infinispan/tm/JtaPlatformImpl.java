/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.tm;

import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

/**
 * @author Steve Ebersole
 */
public class JtaPlatformImpl implements JtaPlatform {
	@Override
	public TransactionManager retrieveTransactionManager(){
		return XaTransactionManagerImpl.getInstance();
	}

	@Override
	public UserTransaction retrieveUserTransaction() {
		throw new TransactionException( "UserTransaction not used in these tests" );
	}

	@Override
	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}

	@Override
	public boolean canRegisterSynchronization() {
		return JtaStatusHelper.isActive( XaTransactionManagerImpl.getInstance() );
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		try {
			XaTransactionManagerImpl.getInstance().getTransaction().registerSynchronization( synchronization );
		}
		catch (Exception e) {
			throw new TransactionException( "Could not obtain transaction from TM" );
		}
	}

	@Override
	public int getCurrentStatus() throws SystemException {
		return JtaStatusHelper.getStatus( XaTransactionManagerImpl.getInstance() );
	}
}
