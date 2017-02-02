/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import java.util.Map;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJtaPlatform
		implements JtaPlatform, Configurable, ServiceRegistryAwareService, TransactionManagerAccess {
	private boolean cacheTransactionManager;
	private boolean cacheUserTransaction;
	private ServiceRegistryImplementor serviceRegistry;

	private final JtaSynchronizationStrategy tmSynchronizationStrategy = new TransactionManagerBasedSynchronizationStrategy( this );

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	protected ServiceRegistry serviceRegistry() {
		return serviceRegistry;
	}

	protected JndiService jndiService() {
		return serviceRegistry().getService( JndiService.class );
	}

	protected abstract TransactionManager locateTransactionManager();
	protected abstract UserTransaction locateUserTransaction();

	public void configure(Map configValues) {
		cacheTransactionManager = ConfigurationHelper.getBoolean(
				AvailableSettings.JTA_CACHE_TM,
				configValues,
				canCacheTransactionManagerByDefault()
		);
		cacheUserTransaction = ConfigurationHelper.getBoolean(
				AvailableSettings.JTA_CACHE_UT,
				configValues,
				canCacheUserTransactionByDefault()
		);
	}

	protected boolean canCacheTransactionManagerByDefault() {
		return true;
	}

	protected boolean canCacheUserTransactionByDefault() {
		return false;
	}

	protected boolean canCacheTransactionManager() {
		return cacheTransactionManager;
	}

	protected boolean canCacheUserTransaction() {
		return cacheUserTransaction;
	}

	private TransactionManager transactionManager;

	@Override
	public TransactionManager retrieveTransactionManager() {
		if ( canCacheTransactionManager() ) {
			if ( transactionManager == null ) {
				transactionManager = locateTransactionManager();
			}
			return transactionManager;
		}
		else {
			return locateTransactionManager();
		}
	}

	@Override
	public TransactionManager getTransactionManager() {
		return retrieveTransactionManager();
	}

	private UserTransaction userTransaction;

	@Override
	public UserTransaction retrieveUserTransaction() {
		if ( canCacheUserTransaction() ) {
			if ( userTransaction == null ) {
				userTransaction = locateUserTransaction();
			}
			return userTransaction;
		}
		return locateUserTransaction();
	}

	@Override
	public Object getTransactionIdentifier(Transaction transaction) {
		// generally we use the transaction itself.
		return transaction;
	}

	protected JtaSynchronizationStrategy getSynchronizationStrategy() {
		return tmSynchronizationStrategy;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		getSynchronizationStrategy().registerSynchronization( synchronization );
	}

	@Override
	public boolean canRegisterSynchronization() {
		return getSynchronizationStrategy().canRegisterSynchronization();
	}

	@Override
	public int getCurrentStatus() throws SystemException {
		return retrieveTransactionManager().getStatus();
	}
}
