/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.selector.spi.StrategyCreator;
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


	public static StrategyCreator STANDARD_STRATEGY_CREATOR = strategyClass -> {
		try {
			return strategyClass.newInstance();
		}
		catch (Exception e) {
			throw new StrategySelectionException(
					String.format( "Could not instantiate named strategy class [%s]", strategyClass.getName() ),
					e
			);
		}
	};

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
			log.trace(
					String.format(
							"Registering named strategy selector [%s] : [%s] -> [%s]",
							strategy.getName(),
							name,
							implementation.getName()
					)
			);
		}
		else {
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

		// try tp clean up afterQuery ourselves...
		if ( namedStrategyImplementorMap.isEmpty() ) {
			namedStrategyImplementorByStrategyMap.remove( strategy );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> selectStrategyImplementor(Class<T> strategy, String name) {
		final Map<String,Class> namedStrategyImplementorMap = namedStrategyImplementorByStrategyMap.get( strategy );
		if ( namedStrategyImplementorMap != null ) {
			final Class registered = namedStrategyImplementorMap.get( name );
			if ( registered != null ) {
				return (Class<T>) registered;
			}
		}

		try {
			return classLoaderService.classForName( name );
		}
		catch (ClassLoadingException e) {
			throw new StrategySelectionException(
					"Unable to resolve name [" + name + "] as strategy [" + strategy.getName() + "]"
			);
		}
	}

	@Override
	public <T> T resolveStrategy(Class<T> strategy, Object strategyReference) {
		return resolveDefaultableStrategy( strategy, strategyReference, (T) null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T resolveDefaultableStrategy(Class<T> strategy, Object strategyReference, final T defaultValue) {
		return resolveDefaultableStrategy(
				strategy,
				strategyReference,
				(Callable<T>) () -> defaultValue
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T resolveDefaultableStrategy(
			Class<T> strategy,
			Object strategyReference,
			Callable<T> defaultResolver) {
		return (T) resolveStrategy( strategy, strategyReference, defaultResolver, STANDARD_STRATEGY_CREATOR );
	}

	@Override
	public <T> T resolveStrategy(
			Class<T> strategy,
			Object strategyReference,
			T defaultValue,
			StrategyCreator<T> creator) {
		return resolveStrategy(
				strategy,
				strategyReference,
				(Callable<T>) () -> defaultValue,
				creator
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T resolveStrategy(
			Class<T> strategy,
			Object strategyReference,
			Callable<T> defaultResolver,
			StrategyCreator<T> creator) {
		if ( strategyReference == null ) {
			try {
				return defaultResolver.call();
			}
			catch (Exception e) {
				throw new StrategySelectionException( "Default-resolver threw exception", e );
			}
		}

		if ( strategy.isInstance( strategyReference ) ) {
			return strategy.cast( strategyReference );
		}

		final Class<? extends T> implementationClass;
		if ( Class.class.isInstance( strategyReference ) ) {
			implementationClass = (Class<T>) strategyReference;
		}
		else {
			implementationClass = selectStrategyImplementor( strategy, strategyReference.toString() );
		}

		try {
			return creator.create( implementationClass );
		}
		catch (Exception e) {
			throw new StrategySelectionException(
					String.format( "Could not instantiate named strategy class [%s]", implementationClass.getName() ),
					e
			);
		}
	}
}
