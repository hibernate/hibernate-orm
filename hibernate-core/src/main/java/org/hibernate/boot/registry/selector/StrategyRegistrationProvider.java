/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector;

import org.hibernate.service.JavaServiceLoadable;

/**
 * Responsible for providing the registrations of one or more strategy selectors.
 * <p>
 * A {@code StrategyRegistrationProvider} may be made available either by:
 * <ul>
 * <li>registering it directly with the
 *     {@link org.hibernate.boot.registry.BootstrapServiceRegistry} by calling
 *     {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder#applyStrategySelectors},
 *     or
 * <li>by making it discoverable via the Java {@link java.util.ServiceLoader} facility.
 * </ul>
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface StrategyRegistrationProvider {
	/**
	 * Get all {@link StrategyRegistration}s announced by this provider.
	 *
	 * @return All {@link StrategyRegistration}s
	 */
	Iterable<StrategyRegistration> getStrategyRegistrations();
}
