/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;
import org.hibernate.internal.util.NullnessUtil;

/**
 * JTA platform implementation intended for use with WebSphere Liberty and OpenLiberty
 *
 * @author Andrew Guibert
 * @author Nathan Rauh
 */
public class WebSphereLibertyJtaPlatform extends AbstractJtaPlatform {

	public static final String TMF_CLASS_NAME = "com.ibm.tx.jta.TransactionManagerFactory";

	public static final String UT_NAME = "java:comp/UserTransaction";

	@Override
	protected TransactionManager locateTransactionManager() {
		try {
			return (TransactionManager) serviceRegistry().requireService( ClassLoaderService.class )
					.classForName( TMF_CLASS_NAME )
					.getMethod("getTransactionManager")
					.invoke(null);
		}
		catch ( Exception e ) {
			throw new JtaPlatformException( "Could not obtain WebSphere Liberty transaction manager instance", e );
		}
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return (UserTransaction) jndiService().locate( UT_NAME );
	}

	@Override
	public boolean canRegisterSynchronization() {
		try {
			return getCurrentStatus() == Status.STATUS_ACTIVE;
		}
		catch (SystemException x) {
			throw new RuntimeException(x);
		}
	}

	@Override
	public int getCurrentStatus() throws SystemException {
		return NullnessUtil.castNonNull( retrieveTransactionManager() ).getStatus();
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		try {
			NullnessUtil.castNonNull( retrieveTransactionManager() ).getTransaction().registerSynchronization(synchronization);
		}
		catch ( RollbackException | SystemException x ) {
			throw new RuntimeException(x);
		}
	}

}
