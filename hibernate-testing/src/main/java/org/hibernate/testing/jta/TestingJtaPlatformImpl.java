/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jta;

import jakarta.transaction.NotSupportedException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JtaSynchronizationStrategy;
import org.hibernate.engine.transaction.jta.platform.internal.SynchronizationRegistryAccess;
import org.hibernate.engine.transaction.jta.platform.internal.SynchronizationRegistryBasedSynchronizationStrategy;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

/**
 * A test-specific implementation of the JtaPlatform contract for testing JTA-based functionality.
 *
 * @author Steve Ebersole
 */
public class TestingJtaPlatformImpl extends AbstractJtaPlatform {
	public static final String NAME = TestingJtaPlatformImpl.class.getName();
	public static final TestingJtaPlatformImpl INSTANCE = new TestingJtaPlatformImpl();

	private final TransactionManager transactionManager;
	private final UserTransaction userTransaction;
	private final TransactionSynchronizationRegistry synchronizationRegistry;

	private final JtaSynchronizationStrategy synchronizationStrategy;

	public TestingJtaPlatformImpl() {
		BeanPopulator
				.getDefaultInstance( ObjectStoreEnvironmentBean.class )
				.setObjectStoreType( VolatileStore.class.getName() );

		BeanPopulator
				.getNamedInstance( ObjectStoreEnvironmentBean.class, "communicationStore" )
				.setObjectStoreType( VolatileStore.class.getName() );

		BeanPopulator
				.getNamedInstance( ObjectStoreEnvironmentBean.class, "stateStore" )
				.setObjectStoreType( VolatileStore.class.getName() );

		transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
		userTransaction = com.arjuna.ats.jta.UserTransaction.userTransaction();
		synchronizationRegistry =
				new com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple();

		synchronizationStrategy = new SynchronizationRegistryBasedSynchronizationStrategy(
				new SynchronizationRegistryAccess() {
					@Override
					public TransactionSynchronizationRegistry getSynchronizationRegistry() {
						return synchronizationRegistry;
					}
				}
		);
	}

	public static TransactionManager transactionManager() {
		return INSTANCE.retrieveTransactionManager();
	}

	public static UserTransaction userTransaction() {
		return INSTANCE.retrieveUserTransaction();
	}

	public static TransactionSynchronizationRegistry synchronizationRegistry() {
		return INSTANCE.synchronizationRegistry;
	}

	/**
	 * Used by envers...
	 */
	public static void tryCommit() throws Exception {
		if ( transactionManager().getStatus() == Status.STATUS_MARKED_ROLLBACK ) {
			transactionManager().rollback();
		}
		else {
			transactionManager().commit();
		}
	}

	public static void inNoopJtaTransaction(TransactionManager tm, Runnable action) throws Exception {
		tm.begin();
		action.run();
		tm.rollback();
	}

	public static void inJtaTransaction(TransactionManager tm, Runnable action) throws Exception {
		inJtaTransaction( tm, -1, action );
	}

	public static void inJtaTransaction(TransactionManager tm, int timeout, Runnable action) throws Exception {
		// account for the timeout, if one was requested
		if ( timeout > 0 ) {
			try {
				tm.setTransactionTimeout( timeout );
			}
			catch (SystemException e) {
				throw new RuntimeException( "Unable to set requested JTA timeout", e );
			}
		}

		try {
			tm.begin();
		}
		catch (NotSupportedException | SystemException e) {
			throw new RuntimeException( "TransactionManager#begin exception", e );
		}

		try {
			action.run();

			tm.commit();
		}
		catch (Exception e) {
			try {
				tm.rollback();
			}
			catch (SystemException ex) {
				throw new RuntimeException( ex );
			}

			throw e;
		}
	}

	@Override
	protected TransactionManager locateTransactionManager() {
		return transactionManager;
	}

	@Override
	protected boolean canCacheTransactionManager() {
		return true;
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return userTransaction;
	}

	@Override
	protected boolean canCacheUserTransaction() {
		return true;
	}

	@Override
	protected JtaSynchronizationStrategy getSynchronizationStrategy() {
		return synchronizationStrategy;
	}

}
