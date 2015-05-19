/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector;

/**
 * Describes the registration of a named strategy implementation.  A strategy + selector name should resolve
 * to a single implementation.
 *
 * @param <T> The type of the strategy described by this implementation registration.
 *
 * @author Steve Ebersole
 */
public interface StrategyRegistration<T> {
	/**
	 * The strategy role.  Best practice says this should be an interface.
	 *
	 * @return The strategy contract/role.
	 */
	public Class<T> getStrategyRole();

	/**
	 * Any registered names for this strategy registration.
	 *
	 * @return The registered selection names.
	 */
	public Iterable<String> getSelectorNames();

	/**
	 * The strategy implementation class.
	 *
	 * @return The strategy implementation.
	 */
	public Class<? extends T> getStrategyImplementation();
}
