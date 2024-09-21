/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vibur.internal;

import java.util.Collections;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * Provides the {@link ViburDBCPConnectionProvider} to the
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service.
 *
 * @author Simeon Malchev
 */
public final class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {

	@Override
	@SuppressWarnings("unchecked")
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		return Collections.singletonList(
				new SimpleStrategyRegistrationImpl<>(
						ConnectionProvider.class,
						ViburDBCPConnectionProvider.class,
						"vibur",
						"viburdbcp",
						ViburDBCPConnectionProvider.class.getSimpleName(),
						// for backward compatibility with pre-existing Vibur project Hibernate integration artifacts
						"org.vibur.dbcp.integration.ViburDBCPConnectionProvider",
						// for consistency's sake
						"org.hibernate.connection.ViburDBCPConnectionProvider"
				) );
	}
}
