/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector;

import java.util.Arrays;

/**
 * A simple implementation of StrategyRegistration.
 *
 * @param <T> The strategy type.
 *
 * @author Steve Ebersole
 */
public class SimpleStrategyRegistrationImpl<T> implements StrategyRegistration<T> {
	private final Class<T> strategyRole;
	private final Class<? extends T> strategyImplementation;
	private final Iterable<String> selectorNames;

	/**
	 * Constructs a SimpleStrategyRegistrationImpl.
	 *
	 * @param strategyRole The strategy contract
	 * @param strategyImplementation The strategy implementation class
	 * @param selectorNames The selection/registration names for this implementation
	 */
	public SimpleStrategyRegistrationImpl(
			Class<T> strategyRole,
			Class<? extends T> strategyImplementation,
			Iterable<String> selectorNames) {
		this.strategyRole = strategyRole;
		this.strategyImplementation = strategyImplementation;
		this.selectorNames = selectorNames;
	}

	/**
	 * Constructs a SimpleStrategyRegistrationImpl.
	 *
	 * @param strategyRole The strategy contract
	 * @param strategyImplementation The strategy implementation class
	 * @param selectorNames The selection/registration names for this implementation
	 */
	public SimpleStrategyRegistrationImpl(
			Class<T> strategyRole,
			Class<? extends T> strategyImplementation,
			String... selectorNames) {
		this( strategyRole, strategyImplementation, Arrays.asList( selectorNames ) );
	}

	@Override
	public Class<T> getStrategyRole() {
		return strategyRole;
	}

	@Override
	public Iterable<String> getSelectorNames() {
		return selectorNames;
	}

	@Override
	public Class<? extends T> getStrategyImplementation() {
		return strategyImplementation;
	}
}
