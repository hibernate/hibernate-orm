/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * StandardServiceInitiator for initiating the TransactionCoordinatorBuilder service.
 *
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class TransactionCoordinatorBuilderInitiator implements StandardServiceInitiator<TransactionCoordinatorBuilder> {
	/**
	 * Singleton access
	 */
	public static final TransactionCoordinatorBuilderInitiator INSTANCE = new TransactionCoordinatorBuilderInitiator();

	@Override
	public TransactionCoordinatorBuilder initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object strategy = configurationValues.get( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY );

		return registry.getService( StrategySelector.class ).resolveDefaultableStrategy(
				TransactionCoordinatorBuilder.class,
				strategy,
				JdbcResourceLocalTransactionCoordinatorBuilderImpl.INSTANCE
		);
	}

	@Override
	public Class<TransactionCoordinatorBuilder> getServiceInitiated() {
		return TransactionCoordinatorBuilder.class;
	}
}
