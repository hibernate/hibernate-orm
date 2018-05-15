/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector.spi;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

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
 * resolving the {@link org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder} implementation to use.  To use the
 * JDBC-based TransactionCoordinatorBuilder the passed name might be either {@code "jdbc"} or
 * {@code "org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl"} (which is the FQN).
 * <p/>
 * Strategy implementations can be managed by {@link #registerStrategyImplementor} and
 * {@link #unRegisterStrategyImplementor}.  Originally designed to help the OSGi use case, though no longer used there.
 * <p/>
 * The service also exposes a general typing API via {@link #resolveStrategy} which accepts implementation references
 * rather than implementation names, allowing for a multitude of interpretations of said "implementation reference".
 * See the docs for {@link #resolveStrategy} for details.
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
	@SuppressWarnings("unused")
	<T> void registerStrategyImplementor(Class<T> strategy, String name, Class<? extends T> implementation);

	/**
	 * Un-registers a named implementor of a particular strategy contract.  Un-registers all named registrations
	 * for the given strategy contract naming the given class.
	 *
	 * @param strategy The strategy contract.
	 * @param implementation The implementation Class
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
	 */
	@SuppressWarnings("unused")
	<T> void unRegisterStrategyImplementor(Class<T> strategy, Class<? extends T> implementation);

	/**
	 * Retrieve all of the registered implementors of the given strategy.  Useful
	 * to allow defaulting the choice to the single registered implementor when
	 * only one is registered
	 *
	 * @return The implementors.  Should never return {@code null}
	 */
	<T> Collection<Class<? extends T>> getRegisteredStrategyImplementors(Class<T> strategy);

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
	<T, I extends T> Class<I> selectStrategyImplementor(Class<T> strategy, String name);

	/**
	 * Resolve strategy instances. If no match is found, {@code null} is returned
	 *
	 * @param strategy The type (interface) of the strategy to be resolved.
	 * @param strategyReference The reference to the strategy for which we need to resolve an instance.
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
	 *
	 * @return The strategy instance
	 */
	<T, I extends T> I resolveStrategy(Class<T> strategy, Object strategyReference);

	<T, I extends T> I resolveStrategy(Class<T> strategy, Object strategyReference, I defaultValue);

	<T, I extends T> I resolveStrategy(Class<T> strategy, Object strategyReference, Supplier<I> defaultValueSupplier);

	<T, I extends T> I resolveStrategy(Class<T> strategy, Object strategyReference, Function<Class<I>,I> creator);

	<T, I extends T> I resolveStrategy(Class<T> strategy, Object strategyReference, I defaultValue, Function<Class<I>,I> creator);

	<T, I extends T> I resolveStrategy(Class<T> strategy, Object strategyReference, Supplier<I> defaultValueSupplier, Function<Class<I>,I> creator);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * @deprecated (since 6.0) - Use one of the {@link #resolveStrategy} forms accepting Supplier rather than Callable
	 */
	@Deprecated
	<T, I extends T> I resolveStrategy(
			Class<T> strategy,
			Object strategyReference,
			Callable<I> defaultValueSupplier,
			Function<Class<I>, I> creator);

	/**
	 * Resolve strategy instances. The incoming reference might be:<ul>
	 * <li>
	 * {@code null} - in which case defaultValue is returned.
	 * </li>
	 * <li>
	 * An actual instance of the strategy type - it is returned, as is
	 * </li>
	 * <li>
	 * A reference to the implementation {@link Class} - an instance is created by calling
	 * {@link Class#newInstance()} (aka, the class's no-arg ctor).
	 * </li>
	 * <li>
	 * The name of the implementation class - First the implementation's {@link Class} reference
	 * is resolved, and then an instance is created by calling {@link Class#newInstance()}
	 * </li>
	 * </ul>
	 *
	 * @param strategy The type (interface) of the strategy to be resolved.
	 * @param strategyReference The reference to the strategy for which we need to resolve an instance.
	 * @param defaultValue THe default value to use if strategyReference is null
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
	 *
	 * @return The strategy instance
	 *
	 * @deprecated (since 6.0) - Use one of the {@link #resolveStrategy} forms
	 */
	@Deprecated
	<T, I extends T> I resolveDefaultableStrategy(Class<T> strategy, Object strategyReference, I defaultValue);

	/**
	 * Same as the other overloaded forms, but here accepting a Supplier for default values.
	 *
	 * @deprecated (since 6.0) - Use one of the {@link #resolveStrategy} forms
	 */
	@Deprecated
	<T> T resolveDefaultableStrategy(Class<T> strategy, Object strategyReference, Supplier<T> defaultValueSupplier);

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
	 * @param defaultResolver A strategy for resolving the default value strategyReference resolves to null.
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
	 *
	 * @return The strategy instance
	 *
	 * @deprecated (since 6.0) - Use one of the {@link #resolveStrategy} forms
	 */
	@Deprecated
	<T, I extends T> I resolveDefaultableStrategy(Class<T> strategy, Object strategyReference, Callable<I> defaultResolver);
}
