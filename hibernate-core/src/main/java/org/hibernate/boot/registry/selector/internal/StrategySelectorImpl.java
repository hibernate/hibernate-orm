/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.selector.spi.StrategyCreator;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;

import org.hibernate.boot.registry.selector.spi.NamedStrategyContributions;
import org.hibernate.boot.registry.selector.spi.NamedStrategyContributor;
import org.jboss.logging.Logger;

/**
 * Standard implementation of the {@link StrategySelector} contract.
 *
 * @implNote Supports both {@linkplain #namedStrategyImplementorByStrategyMap normal}
 * and {@linkplain #lazyStrategyImplementorByStrategyMap lazy} registration.
 *
 * @author Steve Ebersole
 */
public class StrategySelectorImpl implements StrategySelector {
	private static final Logger LOG = Logger.getLogger( StrategySelectorImpl.class );

	private static final StrategyCreator<?> STANDARD_STRATEGY_CREATOR = StrategySelectorImpl::create;

	private final Map<Class<?>,Map<String,Class<?>>> namedStrategyImplementorByStrategyMap = new ConcurrentHashMap<>();
	private final Map<Class<?>, LazyServiceResolver<?>> lazyStrategyImplementorByStrategyMap = new ConcurrentHashMap<>();

	private final ClassLoaderService classLoaderService;
	private final Collection<NamedStrategyContributor> contributors;

	/**
	 * Constructs a StrategySelectorImpl using the given class loader service.
	 *
	 * @param classLoaderService The class loader service usable by this StrategySelectorImpl instance.
	 */
	public StrategySelectorImpl(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;

		this.contributors = classLoaderService.loadJavaServices( NamedStrategyContributor.class );
		for ( NamedStrategyContributor contributor : contributors ) {
			contributor.contributeStrategyImplementations( new StartupContributions() );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> selectStrategyImplementor(Class<T> strategy, String name) {
		final Map<String,Class<?>> namedStrategyImplementorMap = namedStrategyImplementorByStrategyMap.get( strategy );
		if ( namedStrategyImplementorMap != null ) {
			final Class<?> registered = namedStrategyImplementorMap.get( name );
			if ( registered != null ) {
				return (Class<T>) registered;
			}
		}

		final LazyServiceResolver<?> lazyServiceResolver = lazyStrategyImplementorByStrategyMap.get( strategy );
		if ( lazyServiceResolver != null ) {
			final Class<?> resolve = lazyServiceResolver.resolve( name );
			if ( resolve != null ) {
				return (Class<? extends T>) resolve;
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
	public <T> T resolveStrategy(Class<T> strategy, Object strategyReference) {
		return resolveDefaultableStrategy( strategy, strategyReference, (T) null );
	}

	@Override
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
		return resolveStrategy( strategy, strategyReference, defaultResolver, (StrategyCreator<T>) STANDARD_STRATEGY_CREATOR );
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

	@Override
	public <T> Collection<Class<? extends T>> getRegisteredStrategyImplementors(Class<T> strategy) {
		final LazyServiceResolver<?> lazyServiceResolver = lazyStrategyImplementorByStrategyMap.get( strategy );
		if ( lazyServiceResolver != null ) {
			throw new StrategySelectionException( "Can't use this method on for strategy types which are embedded in the core library" );
		}
		final Map<String, Class<?>> registrations = namedStrategyImplementorByStrategyMap.get( strategy );
		if ( registrations == null ) {
			return Collections.emptySet();
		}
		//noinspection unchecked,rawtypes
		return (Collection) new HashSet<>( registrations.values() );
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

		final Class<? extends T> implementationClass =
				strategyReference instanceof Class
						? (Class<? extends T>) strategyReference
						: selectStrategyImplementor( strategy, strategyReference.toString() );

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

	private static <T> T create(Class<T> strategyClass) {
		try {
			return strategyClass.getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw new StrategySelectionException(
					String.format( "Could not instantiate named strategy class [%s]", strategyClass.getName() ),
					e
			);
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Lifecycle

	public <T> void registerStrategyLazily(Class<T> strategy, LazyServiceResolver<T> resolver) {
		LazyServiceResolver<?> previous = lazyStrategyImplementorByStrategyMap.put( strategy, resolver );
		if ( previous != null ) {
			throw new HibernateException( "Detected a second LazyServiceResolver replacing an existing LazyServiceResolver implementation for strategy " + strategy.getName() );
		}
	}

	private <T> void contributeImplementation(Class<T> strategy, Class<? extends T> implementation, String... names) {
		final var namedStrategyImplementorMap =
				namedStrategyImplementorByStrategyMap.computeIfAbsent( strategy, clazz -> new ConcurrentHashMap<>() );

		for ( String name : names ) {
			final Class<?> old = namedStrategyImplementorMap.put( name, implementation );
			if ( LOG.isTraceEnabled() ) {
				if ( old == null ) {
					LOG.tracef(
							"Strategy selector for %s: '%s' -> %s",
							strategy.getSimpleName(),
							name,
							implementation.getName()
					);
				}
				else {
					LOG.tracef(
							"Strategy selector for %s: '%s' -> %s (replacing %s)",
							strategy.getSimpleName(),
							name,
							implementation.getName(),
							old.getName()
					);
				}
			}
		}
	}

	private <T> void removeImplementation(Class<T> strategy, Class<? extends T> implementation) {
		final Map<String,Class<?>> namedStrategyImplementorMap = namedStrategyImplementorByStrategyMap.get( strategy );
		if ( namedStrategyImplementorMap == null ) {
			LOG.debug( "Named strategy map did not exist on call to unregister" );
			return;
		}

		final Iterator<Class<?>> itr = namedStrategyImplementorMap.values().iterator();
		while ( itr.hasNext() ) {
			final Class<?> registered = itr.next();
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
	public void stop() {
		for ( NamedStrategyContributor contributor : contributors ) {
			contributor.clearStrategyImplementations( new ShutdownContributions() );
		}
	}

	private class StartupContributions implements NamedStrategyContributions {
		@Override
		public <T> void contributeStrategyImplementor(Class<T> strategy, Class<? extends T> implementation, String... names) {
			contributeImplementation( strategy, implementation, names );
		}

		@Override
		public <T> void removeStrategyImplementor(Class<T> strategy, Class<? extends T> implementation) {
			removeImplementation( strategy, implementation );
		}
	}

	private class ShutdownContributions extends StartupContributions {
		@Override
		public <T> void contributeStrategyImplementor(Class<T> strategy, Class<? extends T> implementation, String... names) {
			throw new IllegalStateException( "Should not register strategies during shutdown" );
		}
	}

	@Override
	public <T> void registerStrategyImplementor(Class<T> strategy, String name, Class<? extends T> implementation) {
		contributeImplementation( strategy, implementation, name );
	}

	@Override
	public <T> void unRegisterStrategyImplementor(Class<T> strategy, Class<? extends T> implementation) {
		removeImplementation( strategy, implementation );
	}
}
