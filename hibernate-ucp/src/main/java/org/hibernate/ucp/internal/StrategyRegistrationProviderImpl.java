/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.ucp.internal;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import static java.util.Collections.singleton;

/**
 * Provides the {@link UCPConnectionProvider} to the
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service.
 *
 * @author Loïc Lefèvre
 */
public class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {
	@Override
	public Iterable<StrategyRegistration<?>> getStrategyRegistrations() {
		return singleton( new SimpleStrategyRegistrationImpl<>(
				ConnectionProvider.class,
				UCPConnectionProvider.class,
				"ucp",
				"oracleucp",
				UCPConnectionProvider.class.getSimpleName(),
				// for consistency's sake
				"org.hibernate.connection.UCPConnectionProvider"
		) );
	}
}
