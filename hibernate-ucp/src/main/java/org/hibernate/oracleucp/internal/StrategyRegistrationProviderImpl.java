/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.oracleucp.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * Provides the {@link UCPConnectionProvider} to the
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service.
 *
 */
public class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {
	private static final List<StrategyRegistration> REGISTRATIONS = Collections.singletonList(
			(StrategyRegistration) new SimpleStrategyRegistrationImpl<ConnectionProvider>(
					ConnectionProvider.class,
					UCPConnectionProvider.class,
					"ucp",
					"oracleucp",
					UCPConnectionProvider.class.getSimpleName(),
					"org.oracle.ucp.connection.UCPConnectionProvider"
			)
	);

	@Override
	@SuppressWarnings("unchecked")
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		return REGISTRATIONS;
	}
}
