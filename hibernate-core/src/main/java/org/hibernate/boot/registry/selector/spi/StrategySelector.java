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
package org.hibernate.boot.registry.selector.spi;

import org.hibernate.service.Service;

/**
 * Service which acts as a registry for named strategy implementations.
 *
 * @author Steve Ebersole
 */
public interface StrategySelector extends Service {
	/**
	 * Registers a named implementor of a particular strategy contract.
	 *
	 * @param strategy The strategy contract.
	 * @param name The registration name
	 * @param implementation The implementation Class
	 */
	public <T> void registerStrategyImplementor(Class<T> strategy, String name, Class<? extends T> implementation);

	/**
	 * Un-registers a named implementor of a particular strategy contract.  Un-registers all named registrations
	 * for the given strategy contract naming the given class.
	 *
	 * @param strategy The strategy contract.
	 * @param implementation The implementation Class
	 */
	public <T> void unRegisterStrategyImplementor(Class<T> strategy, Class<? extends T> implementation);

	/**
	 * Locate the named strategy implementation.
	 *
	 * @param strategy The type of strategy to be resolved.
	 * @param name The name of the strategy to locate; might be either a registered name or the implementation FQN.
	 *
	 * @return The named strategy implementation class.
	 */
	public <T> Class<? extends T> selectStrategyImplementor(Class<T> strategy, String name);

	/**
	 * Resolve strategy instances. See discussion on {@link #resolveDefaultableStrategy}.
	 * Only difference is that here, the implied default value is {@code null}.
	 *
	 * @param strategy The type (interface) of the strategy to be resolved.
	 * @param strategyReference The reference to the strategy for which we need to resolve an instance.
	 *
	 * @return The strategy instance
	 */
	public <T> T resolveStrategy(Class<T> strategy, Object strategyReference);

	/**
	 * Resolve strategy instances. The incoming reference might be:<ul>
	 *     <li>
	 *         {@code null} - in which case defaultValue is returned.
	 *     </li>
	 *     <li>
	 *         An actual instance of the strategy type - it is returned, as is
	 *     </li>
	 *     <li>
	 *         A reference to the implementation {@link Class} - an instance is created by calling
	 *         {@link Class#newInstance()} (aka, the class's no-arg ctor).
	 *     </li>
	 *     <li>
	 *         The name of the implementation class - First the implementation's {@link Class} reference
	 *         is resolved, and then an instance is created by calling {@link Class#newInstance()}
	 *     </li>
	 * </ul>
	 *
	 * @param strategy The type (interface) of the strategy to be resolved.
	 * @param strategyReference The reference to the strategy for which we need to resolve an instance.
	 * @param defaultValue THe default value to use if strategyReference is null
	 *
	 * @return The strategy instance
	 */
	public <T> T resolveDefaultableStrategy(Class<T> strategy, Object strategyReference, T defaultValue);
}
