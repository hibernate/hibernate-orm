/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.classloading.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * A service loader bound to an {@link AggregatedClassLoader}.
 * @param <S> The type of the service contract.
 */
abstract class AggregatedServiceLoader<S> {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( AggregatedServiceLoader.class );

	static <S> AggregatedServiceLoader<S> create(AggregatedClassLoader aggregatedClassLoader,
			Class<S> serviceContract) {
			return new ClassPathAndModulePathAggregatedServiceLoader<>( aggregatedClassLoader, serviceContract );
	}

	/**
	 * @return All the loaded services.
	 */
	public abstract Collection<S> getAll();

	/**
	 * Release all resources.
	 */
	public abstract void close();

	/**
	 * An {@link AggregatedServiceLoader} that will detect services defined in the class path or in the module path.
	 * <p>
	 * This implementation only works when running Hibernate ORM on Java 9 and above.
	 * <p>
	 * When retrieving services from providers in the module path,
	 * the service loader internally uses a map from classloader to service catalog.
	 * Since the aggregated class loader is artificial and unknown from the service loader,
	 * it will never match any service from the module path.
	 * <p>
	 * To work around this problem,
	 * we try to get services from a service loader bound to the aggregated class loader first,
	 * then we try a service loader bound to each individual class loader.
	 * <p>
	 * This could result in duplicates, so we take specific care to avoid using the same service provider twice.
	 * See {@link #getAll()}.
	 * <p>
	 * Note that, in the worst case,
	 * the service retrieved from each individual class loader could load duplicate versions
	 * of classes already loaded from another class loader.
	 * For example in an aggregated class loader made up of individual class loader A, B, C:
	 * it is possible that class C1 was already loaded from A,
	 * but then we load service S1 from B, and this service will also need class C1 but won't find it in class loader B,
	 * so it will load its own version of that class.
	 * <p>
	 * We assume that this situation will never occur in practice because class loaders
	 * are structure in a hierarchy that prevents one class to be loaded twice.
	 *
	 * @param <S> The type of loaded services.
	 */
	private static class ClassPathAndModulePathAggregatedServiceLoader<S> extends AggregatedServiceLoader<S> {
		private final Class<S> serviceContract;
		private final ServiceLoader<S> aggregatedClassLoaderServiceLoader;
		private final List<ServiceLoader<S>> delegates;
		private Collection<S> cache = null;

		private ClassPathAndModulePathAggregatedServiceLoader(AggregatedClassLoader aggregatedClassLoader,
				Class<S> serviceContract) {
			this.serviceContract = serviceContract;
			this.delegates = new ArrayList<>();
			this.aggregatedClassLoaderServiceLoader = ServiceLoader.load( serviceContract, aggregatedClassLoader );
			final Iterator<ClassLoader> clIterator = aggregatedClassLoader.newClassLoaderIterator();
			while ( clIterator.hasNext() ) {
				this.delegates.add(
						ServiceLoader.load( serviceContract, clIterator.next() )
				);
			}
		}

		@Override
		public Collection<S> getAll() {
			if ( cache == null ) {
				/*
				 * loadAll() uses ServiceLoader.Provider.get(), which doesn't cache the instance internally,
				 * on contrary to ServiceLoader.iterator().
				 * Looking at how https://hibernate.atlassian.net/browse/HHH-8363 was solved,
				 * waiting for Hibernate ORM to shut down before clearing the service caches,
				 * it seems caching of service instances is important, or at least used to be important.
				 * Thus we cache service instances ourselves to avoid any kind of backward-incompatibility.
				 * If one day we decide caching isn't important, this cache can be removed,
				 * as well as the close() method,
				 * and also the service loader map in ClassLoaderServiceImpl,
				 * and we can simply call .reload() on the service loader after we load services
				 * in ClassPathAndModulePathAggregatedServiceLoader.getAll().
				 */
				cache = Collections.unmodifiableCollection( loadAll() );
			}
			return cache;
		}

		private Collection<S> loadAll() {
			Set<String> alreadyEncountered = new HashSet<>();
			Set<S> result = new LinkedHashSet<>();

			// Always try the aggregated class loader first
			var providerIterator = aggregatedClassLoaderServiceLoader.stream().iterator();
			while ( providerIterator.hasNext() ) {
				ServiceLoader.Provider<S> provider = providerIterator.next();
				collectServiceIfNotDuplicate( result, alreadyEncountered, provider );
			}

			/*
			 * Then also try the individual class loaders,
			 * because only them can instantiate services provided by jars in the module path.
			 */
			for ( ServiceLoader<S> delegate : delegates ) {
				providerIterator = delegate.stream().iterator();
				/*
				 * Note that advancing the stream itself can lead to (arguably) "legitimate" errors,
				 * where we fail to load the service, but only because each individual classloader
				 * has its own definition of the service contract class, which is different from ours.
				 * In that case (still arguably), the error should be ignored.
				 * That's why we wrap the call to hasNext in a method that catches and logs errors.
				 * See https://hibernate.atlassian.net/browse/HHH-13551.
				 */
				while ( hasNextIgnoringServiceConfigurationError( providerIterator ) ) {
					final ServiceLoader.Provider<S> provider = providerIterator.next();
					collectServiceIfNotDuplicate( result, alreadyEncountered, provider );
				}
			}

			return result;
		}

		private boolean hasNextIgnoringServiceConfigurationError(Iterator<?> iterator) {
			while ( true ) {
				try {
					return iterator.hasNext();
				}
				catch (ServiceConfigurationError e) {
					log.ignoringServiceConfigurationError( serviceContract.getName(), e );
				}
			}
		}

		/*
		 * We may encounter the same service provider multiple times,
		 * because the individual class loaders may give access to the same types
		 * (at the very least a single class loader may be present twice in the aggregated class loader).
		 * However, we only want to get the service from each provider once.
		 *
		 * ServiceLoader.stream() is useful in that regard,
		 * since it allows us to check the type of the service provider
		 * before the service is even instantiated.
		 *
		 * We could just instantiate every service and check their type afterwards,
		 * but 1. it would lead to unnecessary instantiation which could have side effects,
		 * in particular regarding class loading,
		 * and 2. the type of the provider may not always be the type of the service,
		 * and one provider may return different types of services
		 * depending on conditions known only to itself.
		 */
		private void collectServiceIfNotDuplicate(Set<S> result, Set<String> alreadyEncountered, ServiceLoader.Provider<S> provider) {
			final Class<? extends S> type = provider.type();
			String typeName = type.getName();
			if ( alreadyEncountered.add( typeName ) ) {
				result.add( provider.get() );
			}
		}

		@Override
		public void close() {
			cache = null;
		}
	}
}
