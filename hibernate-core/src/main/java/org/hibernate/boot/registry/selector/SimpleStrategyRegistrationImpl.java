/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
