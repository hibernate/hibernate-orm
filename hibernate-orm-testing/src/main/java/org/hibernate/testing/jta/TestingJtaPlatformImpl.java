/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jta;

import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

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
