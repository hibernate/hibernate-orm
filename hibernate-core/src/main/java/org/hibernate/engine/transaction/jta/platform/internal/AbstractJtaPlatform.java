/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import java.util.Map;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static org.hibernate.cfg.TransactionSettings.JTA_CACHE_TM;
import static org.hibernate.cfg.TransactionSettings.JTA_CACHE_UT;
import static org.hibernate.engine.transaction.internal.jta.JtaStatusHelper.isActive;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJtaPlatform
		implements JtaPlatform, Configurable, ServiceRegistryAwareService, TransactionManagerAccess {
	private boolean cacheTransactionManager;
	private boolean cacheUserTransaction;
	private ServiceRegistryImplementor serviceRegistry;

	private final JtaSynchronizationStrategy tmSynchronizationStrategy = new TransactionManagerBasedSynchronizationStrategy();

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	private final class TransactionManagerBasedSynchronizationStrategy implements JtaSynchronizationStrategy {

		@Override
		public void registerSynchronization(Synchronization synchronization) {
			try {
				getTransactionManager().getTransaction().registerSynchronization( synchronization );
			}
			catch (Exception e) {
				throw new JtaPlatformException( "Could not access JTA Transaction to register synchronization", e );
			}
		}

		@Override
		public boolean canRegisterSynchronization() {
			return isActive( getTransactionManager() );
		}
	}

	protected ServiceRegistry serviceRegistry() {
		return serviceRegistry;
	}

	protected JndiService jndiService() {
		return serviceRegistry().requireService( JndiService.class );
	}

	protected abstract TransactionManager locateTransactionManager();
	protected abstract UserTransaction locateUserTransaction();

	public void configure(Map<String, Object> configValues) {
		cacheTransactionManager = getBoolean( JTA_CACHE_TM, configValues, canCacheTransactionManagerByDefault() );
		cacheUserTransaction = getBoolean( JTA_CACHE_UT, configValues, canCacheUserTransactionByDefault() );
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
		else {
			return locateUserTransaction();
		}
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
