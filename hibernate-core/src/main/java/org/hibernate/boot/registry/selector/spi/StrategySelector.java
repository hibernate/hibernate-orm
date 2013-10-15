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
 * <p/>
 * Strategies are more open ended than services, though a strategy managed here might very well also be a service.  The
 * strategy is any interface that has multiple, (possibly short) named implementations.
 * <p/>
 * StrategySelector manages resolution of particular implementation by (possibly short) name via the
 * {@link #selectStrategyImplementor} method, which is the main contract here.  As indicated in the docs of that
 * method the given name might be either a short registered name or the implementation FQN.  As an example, consider
 * resolving the {@link org.hibernate.engine.transaction.spi.TransactionFactory} implementation to use.  To use the
 * JDBC-based TransactionFactory the passed name might be either {@code "jdbc"} or
 * {@code "org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory"} (which is the FQN).
 * <p/>
 * Strategy implementations can be managed by {@link #registerStrategyImplementor} and
 * {@link #unRegisterStrategyImplementor}.  Originally designed to help the OSGi use case, though no longer used there.
 * <p/>
 * The service also exposes a general typing API via {@link #resolveStrategy} and {@link #resolveDefaultableStrategy}
 * which accept implementation references rather than implementation names, allowing for a multitude of interpretations
 * of said "implementation reference".  See the docs for {@link #resolveDefaultableStrategy} for details.
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
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
	 */
	public <T> void registerStrategyImplementor(Class<T> strategy, String name, Class<? extends T> implementation);

	/**
	 * Un-registers a named implementor of a particular strategy contract.  Un-registers all named registrations
	 * for the given strategy contract naming the given class.
	 *
	 * @param strategy The strategy contract.
	 * @param implementation The implementation Class
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
	 */
	public <T> void unRegisterStrategyImplementor(Class<T> strategy, Class<? extends T> implementation);

	/**
	 * Locate the named strategy implementation.
	 *
	 * @param strategy The type of strategy to be resolved.
	 * @param name The name of the strategy to locate; might be either a registered name or the implementation FQN.
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
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
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
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
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
	 *
	 * @return The strategy instance
	 */
	public <T> T resolveDefaultableStrategy(Class<T> strategy, Object strategyReference, T defaultValue);
}
