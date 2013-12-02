/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
