/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hikaricp.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * Provides the {@link HikariCPConnectionProvider} to the
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service.
 * 
 * @author Brett Meyer
 */
public class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {
	private static final List<StrategyRegistration> REGISTRATIONS = Collections.singletonList(
			(StrategyRegistration) new SimpleStrategyRegistrationImpl<ConnectionProvider>(
					ConnectionProvider.class,
					HikariCPConnectionProvider.class,
					"hikari",
					"hikaricp",
					HikariCPConnectionProvider.class.getSimpleName(),
					// for consistency's sake
					"org.hibernate.connection.HikariCPConnectionProvider"
			)
	);

	@Override
	@SuppressWarnings("unchecked")
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		return REGISTRATIONS;
	}
}
