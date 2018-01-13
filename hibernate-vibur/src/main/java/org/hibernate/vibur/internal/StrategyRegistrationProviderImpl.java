/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.vibur.internal;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import java.util.Collections;
import java.util.List;

/**
 * Provides the {@link ViburDBCPConnectionProvider} to the
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service.
 * 
 * @author Simeon Malchev
 */
public class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {
	private static final List<StrategyRegistration> REGISTRATIONS = Collections.singletonList(
			(StrategyRegistration) new SimpleStrategyRegistrationImpl<>(
					ConnectionProvider.class,
					ViburDBCPConnectionProvider.class,
					"vibur",
					"viburdbcp",
					ViburDBCPConnectionProvider.class.getSimpleName(),
					// for backward compatibility with pre-existing Vibur project Hibernate integration artifacts
					"org.vibur.dbcp.integration.ViburDBCPConnectionProvider",
					// for consistency's sake
					"org.hibernate.connection.ViburDBCPConnectionProvider"
			)
	);

	@Override
	@SuppressWarnings("unchecked")
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		return REGISTRATIONS;
	}
}
