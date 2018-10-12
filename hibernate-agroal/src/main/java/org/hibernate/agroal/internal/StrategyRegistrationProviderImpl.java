/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.agroal.internal;

import java.util.Collections;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * Provides the {@link AgroalConnectionProvider} to the
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service.
 *
 * @author Luis Barreiro
 */
public final class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {

	@Override
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		final SimpleStrategyRegistrationImpl<ConnectionProvider> strategyRegistration = new SimpleStrategyRegistrationImpl<>(
				ConnectionProvider.class,
				AgroalConnectionProvider.class,
				AgroalConnectionProvider.class.getSimpleName(),
				"agroal",
				"Agroal",
				// for consistency's sake
				"org.hibernate.connection.AgroalConnectionProvider"
		);
		return Collections.singleton( strategyRegistration );
	}
}
