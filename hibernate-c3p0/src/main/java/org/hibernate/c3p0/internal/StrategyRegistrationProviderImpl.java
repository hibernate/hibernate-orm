/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.c3p0.internal;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import static java.util.Collections.singleton;

/**
 * Provides the {@link C3P0ConnectionProvider} to the
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service.
 *
 * @author Brett Meyer
 */
public final class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {

	@Override
	public Iterable<StrategyRegistration<?>> getStrategyRegistrations() {
		return singleton( new SimpleStrategyRegistrationImpl<>(
				ConnectionProvider.class,
				C3P0ConnectionProvider.class,
				"c3p0",
				C3P0ConnectionProvider.class.getSimpleName(),
				// legacy
				"org.hibernate.connection.C3P0ConnectionProvider",
				// legacy
				"org.hibernate.service.jdbc.connections.internal.C3P0ConnectionProvider"
		) );
	}
}
