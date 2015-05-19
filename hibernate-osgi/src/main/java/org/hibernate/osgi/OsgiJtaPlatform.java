/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi;

import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

/**
 * Offers the JTA Platform provided by the OSGi container. The Enterprise
 * OSGi spec requires all containers to register UserTransaction
 * and TransactionManager OSGi services.
 * 
 * @author Brett Meyer
 */
public class OsgiJtaPlatform implements JtaPlatform {
	private static final long serialVersionUID = 1L;
	
	private OsgiServiceUtil osgiServiceUtil;

	/**
	 * Constructs a OsgiJtaPlatform
	 *
	 * @param bundleContext The OSGi bundle context
	 */
	public OsgiJtaPlatform(OsgiServiceUtil osgiServiceUtil) {
		this.osgiServiceUtil = osgiServiceUtil;
	}

	@Override
	public TransactionManager retrieveTransactionManager() {
		try {
			final TransactionManager transactionManager = osgiServiceUtil.getServiceImpl(
					TransactionManager.class );
			if (transactionManager == null) {
				throw new TransactionException("Cannot retrieve the TransactionManager OSGi service!");
			}
			return transactionManager;
		}
		catch (Exception e) {
			throw new TransactionException("Cannot retrieve the TransactionManager OSGi service!", e);
		}
	}

	@Override
	public UserTransaction retrieveUserTransaction() {
		try {
			final UserTransaction userTransaction = osgiServiceUtil.getServiceImpl(
					UserTransaction.class );
			if (userTransaction == null) {
				throw new TransactionException("Cannot retrieve the UserTransaction OSGi service!");
			}
			return userTransaction;
		}
		catch (Exception e) {
			throw new TransactionException("Cannot retrieve the UserTransaction OSGi service!", e);
		}
	}

	@Override
	public Object getTransactionIdentifier(Transaction transaction) {
		// AbstractJtaPlatform just uses the transaction itself.
		return transaction;
	}

	@Override
	public boolean canRegisterSynchronization() {
		try {
			return JtaStatusHelper.isActive( retrieveTransactionManager() );
		}
		catch (Exception e) {
			return false;
		}
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		try {
			retrieveTransactionManager().getTransaction().registerSynchronization( synchronization );
		}
		catch (Exception e) {
			throw new TransactionException( "Could not obtain transaction from OSGi services!" );
		}
	}

	@Override
	public int getCurrentStatus() throws SystemException {
		return retrieveTransactionManager().getStatus();
	}

}
