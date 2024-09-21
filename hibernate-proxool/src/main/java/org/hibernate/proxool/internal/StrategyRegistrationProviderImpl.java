/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxool.internal;

import java.util.Collections;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * Provides the {@link ProxoolConnectionProvider} to the
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service.
 *
 * @author Brett Meyer
 */
public final class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {

	@Override
	@SuppressWarnings("unchecked")
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		return Collections.singletonList(
				new SimpleStrategyRegistrationImpl<ConnectionProvider>(
						ConnectionProvider.class,
						ProxoolConnectionProvider.class,
						"proxool",
						ProxoolConnectionProvider.class.getSimpleName(),
						// legacy
						"org.hibernate.connection.ProxoolConnectionProvider",
						// legacy
						"org.hibernate.service.jdbc.connections.internal.ProxoolConnectionProvider"
				) );
	}
}
