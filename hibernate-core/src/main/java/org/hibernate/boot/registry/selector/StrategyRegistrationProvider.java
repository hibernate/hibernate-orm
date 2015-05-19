/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector;

/**
 * Responsible for providing the registrations of strategy selector(s).  Can be registered directly with the
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistry} or located via discovery.
 *
 * @author Steve Ebersole
 */
public interface StrategyRegistrationProvider {
	/**
	 * Get all StrategyRegistrations announced by this provider.
	 *
	 * @return All StrategyRegistrations
	 */
	public Iterable<StrategyRegistration> getStrategyRegistrations();
}
