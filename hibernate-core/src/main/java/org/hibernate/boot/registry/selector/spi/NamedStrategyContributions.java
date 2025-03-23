/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.spi;

/**
 * Target for {@linkplain NamedStrategyContributor}
 *
 * @see StrategySelector
 *
 * @author Steve Ebersole
 */
public interface NamedStrategyContributions {
	/**
	 * Registers a named implementor of a particular strategy contract.
	 *
	 * @param strategy The strategy contract.
	 * @param implementation The implementation class.
	 * @param names The registration names.
	 *
	 * @param <T> The strategy type.
	 */
	<T> void contributeStrategyImplementor(Class<T> strategy, Class<? extends T> implementation, String... names);

	/**
	 * Un-registers a named implementor of a particular strategy contract.  Un-registers all named registrations
	 * for the given strategy contract naming the given class.
	 *
	 * @param strategy The strategy contract.
	 * @param implementation The implementation class.
	 *
	 * @param <T> The strategy type.
	 */
	<T> void removeStrategyImplementor(Class<T> strategy, Class<? extends T> implementation);
}
