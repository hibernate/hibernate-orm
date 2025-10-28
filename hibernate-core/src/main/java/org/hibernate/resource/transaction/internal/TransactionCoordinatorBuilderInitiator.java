/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static org.hibernate.cfg.TransactionSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * StandardServiceInitiator for initiating the TransactionCoordinatorBuilder service.
 *
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class TransactionCoordinatorBuilderInitiator implements StandardServiceInitiator<TransactionCoordinatorBuilder> {
	public static final String LEGACY_SETTING_NAME = "hibernate.transaction.factory_class";

	/**
	 * Singleton access
	 */
	public static final TransactionCoordinatorBuilderInitiator INSTANCE = new TransactionCoordinatorBuilderInitiator();

	@Override
	public TransactionCoordinatorBuilder initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		return registry.requireService( StrategySelector.class )
				.resolveDefaultableStrategy( TransactionCoordinatorBuilder.class,
						determineStrategySelection( configurationValues ),
						JdbcResourceLocalTransactionCoordinatorBuilderImpl.INSTANCE );
	}

	private static Object determineStrategySelection(Map<String, Object> configurationValues) {
		final Object coordinatorStrategy = configurationValues.get( TRANSACTION_COORDINATOR_STRATEGY );
		if ( coordinatorStrategy != null ) {
			return coordinatorStrategy;
		}

		final Object legacySetting = configurationValues.get( LEGACY_SETTING_NAME );
		if ( legacySetting != null ) {
			DEPRECATION_LOGGER.logDeprecatedTransactionFactorySetting(
					LEGACY_SETTING_NAME,
					TRANSACTION_COORDINATOR_STRATEGY
			);
			return legacySetting;
		}

		// triggers the default
		return null;
	}

	@Override
	public Class<TransactionCoordinatorBuilder> getServiceInitiated() {
		return TransactionCoordinatorBuilder.class;
	}
}
