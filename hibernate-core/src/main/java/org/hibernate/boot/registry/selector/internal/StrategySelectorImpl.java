/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;

import org.jboss.logging.Logger;

/**
 * Standard implementation of the StrategySelector contract.
 *
 * @author Steve Ebersole
 */
public class StrategySelectorImpl implements StrategySelector {
	private static final Logger log = Logger.getLogger( StrategySelectorImpl.class );

	private static final StandardCreator STANDARD_CREATOR = new StandardCreator();

	@SuppressWarnings("unchecked")
	public static <I> StandardCreator<I> getStandardCreator() {
		return STANDARD_CREATOR;
	}

	private final Map<Class,Map<String,Class>> namedStrategyImplementorByStrategyMap = new ConcurrentHashMap<>();

	private final ClassLoaderService classLoaderService;

	/**
	 * Constructs a StrategySelectorImpl using the given class loader service.
	 *
	 * @param classLoaderService The class loader service usable by this StrategySelectorImpl instance.
	 */
	public StrategySelectorImpl(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

	@Override
	public <T> void registerStrategyImplementor(Class<T> strategy, String name, Class<? extends T> implementation) {
		Map<String,Class> namedStrategyImplementorMap = namedStrategyImplementorByStrategyMap.get( strategy );
		if ( namedStrategyImplementorMap == null ) {
			namedStrategyImplementorMap = new ConcurrentHashMap<>();
			namedStrategyImplementorByStrategyMap.put( strategy, namedStrategyImplementorMap );
		}

		final Class old = namedStrategyImplementorMap.put( name, implementation );
		if ( old == null ) {
			if ( log.isTraceEnabled() ) {
				log.trace(
						String.format(
								"Registering named strategy selector [%s] : [%s] -> [%s]",
								strategy.getName(),
								name,
								implementation.getName()
						)
				);
			}
		}
		else {
			if ( log.isDebugEnabled() ) {
				log.debug(
						String.format(
								"Registering named strategy selector [%s] : [%s] -> [%s] (replacing [%s])",
								strategy.getName(),
								name,
								implementation.getName(),
								old.getName()
						)
				);
			}
		}
	}

	@Override
	public <T> void unRegisterStrategyImplementor(Class<T> strategy, Class<? extends T> implementation) {
		final Map<String,Class> namedStrategyImplementorMap = namedStrategyImplementorByStrategyMap.get( strategy );
		if ( namedStrategyImplementorMap == null ) {
			log.debug( "Named strategy map did not exist on call to un-register" );
			return;
		}

		final Iterator itr = namedStrategyImplementorMap.values().iterator();
		while ( itr.hasNext() ) {
			final Class registered = (Class) itr.next();
			if ( registered.equals( implementation ) ) {
				itr.remove();
			}
		}

		// try to clean up after ourselves...
		if ( namedStrategyImplementorMap.isEmpty() ) {
			namedStrategyImplementorByStrategyMap.remove( strategy );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection getRegisteredStrategyImplementors(Class strategy) {
		final Map<String, Class> registrations = namedStrategyImplementorByStrategyMap.get( strategy );
		if ( registrations == null ) {
			return Collections.emptySet();
		}
		return new HashSet( registrations.values() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, I extends T> Class<I> selectStrategyImplementor(Class<T> strategy, String name) {
		final Map<String,Class> namedStrategyImplementorMap = namedStrategyImplementorByStrategyMap.get( strategy );
		if ( namedStrategyImplementorMap != null ) {
			final Class registered = namedStrategyImplementorMap.get( name );
			if ( registered != null ) {
				return registered;
			}
		}

		try {
			return classLoaderService.classForName( name );
		}
		catch (ClassLoadingException e) {
			throw new StrategySelectionException(
					"Unable to resolve name [" + name + "] as strategy [" + strategy.getName() + "]",
					e
			);
		}
	}

	@Override
	public <T, I extends T> I resolveStrategy(Class<T> strategy, Object strategyReference) {
		return resolveStrategy( strategy, strategyReference, getStandardCreator() );
	}

	@Override
	public <T, I extends T> I resolveStrategy(Class<T> strategy, Object strategyReference, I defaultValue) {
		return resolveStrategy(
				strategy,
				strategyReference,
				(Supplier<I>) () -> defaultValue,
				getStandardCreator()
		);
	}

	@Override
	public <T, I extends T> I resolveStrategy(Class<T> strategy, Object strategyReference, Supplier<I> defaultValueSupplier) {
		return resolveStrategy(
				strategy,
				strategyReference,
				defaultValueSupplier,
				getStandardCreator()
		);
	}

	@Override
	public <T, I extends T> I resolveStrategy(Class<T> strategy, Object strategyReference, Function<Class<I>, I> creator) {
		return resolveStrategy( strategy, strategyReference, (I) null, creator );
	}

	@Override
	public <T, I extends T> I resolveStrategy(
			Class<T> strategy,
			Object strategyReference,
			I defaultValue,
			Function<Class<I>,I> creator) {
		return resolveStrategy(
				strategy,
				strategyReference,
				(Supplier<I>) () -> defaultValue,
				creator
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, I extends T> I resolveStrategy(
			Class<T> strategy,
			Object strategyReference,
			Supplier<I> defaultValueSupplier,
			Function<Class<I>, I> creator) {
		if ( strategyReference == null ) {
			try {
				return defaultValueSupplier.get();
			}
			catch (Exception e) {
				throw new StrategySelectionException( "Default value supplier threw exception", e );
			}
		}

		if ( strategy.isInstance( strategyReference ) ) {
			return (I) strategy.cast( strategyReference );
		}

		final Class<I> implementationClass;
		if ( Class.class.isInstance( strategyReference ) ) {
			implementationClass = (Class<I>) strategyReference;
		}
		else {
			implementationClass = selectStrategyImplementor( strategy, strategyReference.toString() );
		}

		try {
			return creator.apply( implementationClass );
		}
		catch (Exception e) {
			throw new StrategySelectionException(
					String.format( "Could not instantiate named strategy class [%s]", implementationClass.getName() ),
					e
			);
		}
	}



	private static class StandardCreator<I> implements Function<Class<I>,I> {
		@Override
		public I apply(Class<I> strategyImplClass) {
			try {
				return strategyImplClass.newInstance();
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new StrategySelectionException(
						String.format( "Could not instantiate named strategy class [%s]", strategyImplClass.getName() ),
						e
				);
			}
		}
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	@Override
	@SuppressWarnings("unchecked")
	public <T, I extends T> I resolveStrategy(
			Class<T> strategy,
			Object strategyReference,
			Callable<I> defaultValueSupplier,
			Function<Class<I>,I> creator) {
		return resolveStrategy(
				strategy,
				strategyReference,
				supplier( defaultValueSupplier ),
				creator
		);
	}

	/**
	 * @deprecated private+deprecated
	 */
	@Deprecated
	private <I> Supplier<I> supplier(Callable<I> callableForm) {
		return () -> {
			try {
				return callableForm.call();
			}
			catch (Exception e) {
				throw new StrategySelectionException( "Error calling Callable to determine strategy default value", e );
			}
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, I extends T> I resolveDefaultableStrategy(Class<T> strategy, Object strategyReference, I defaultValue) {
		return resolveStrategy(
				strategy,
				strategyReference,
				(Supplier<I>) () -> defaultValue
		);
	}

	@Override
	public <T> T resolveDefaultableStrategy(
			Class<T> strategy, Object strategyReference, Supplier<T> defaultValueSupplier) {
		return resolveDefaultableStrategy(
				strategy,
				strategyReference,
				(Callable<T>) () -> defaultValueSupplier.get()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, I extends T> I resolveDefaultableStrategy(
			Class<T> strategy,
			Object strategyReference,
			Callable<I> defaultResolver) {
		return (I) resolveStrategy( strategy, strategyReference, defaultResolver, getStandardCreator() );
	}
}
