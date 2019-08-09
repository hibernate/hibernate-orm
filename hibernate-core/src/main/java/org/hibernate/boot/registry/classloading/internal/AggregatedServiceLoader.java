/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.registry.classloading.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * A service loader bound to an {@link AggregatedClassLoader}.
 * @param <S> The type of the service contract.
 */
abstract class AggregatedServiceLoader<S> {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( AggregatedServiceLoader.class );

	private static final Method SERVICE_LOADER_STREAM_METHOD;
	private static final Method PROVIDER_TYPE_METHOD;

	static {
		Class<?> serviceLoaderClass = ServiceLoader.class;
		Method serviceLoaderStreamMethod = null;
		Method providerTypeMethod = null;
		try {
			/*
			 * JDK 9 introduced the stream() method on ServiceLoader,
			 * which we need in order to avoid duplicate service instantiation.
			 * See ClassPathAndModulePathAggregatedServiceLoader.
			 */
			serviceLoaderStreamMethod = serviceLoaderClass.getMethod( "stream" );
			Class<?> providerClass = Class.forName( serviceLoaderClass.getName() + "$Provider" );
			providerTypeMethod = providerClass.getMethod( "type" );
		}
		catch (NoSuchMethodException | ClassNotFoundException e) {
			/*
			 * Probably Java 8.
			 * Leave the method constants null,
			 * we will automatically use a service loader implementation that doesn't rely on them.
			 * See create(...).
			 */
		}

		SERVICE_LOADER_STREAM_METHOD = serviceLoaderStreamMethod;
		PROVIDER_TYPE_METHOD = providerTypeMethod;
	}

	static <S> AggregatedServiceLoader<S> create(AggregatedClassLoader aggregatedClassLoader,
			Class<S> serviceContract) {
		if ( SERVICE_LOADER_STREAM_METHOD != null ) {
			// Java 9+
			return new ClassPathAndModulePathAggregatedServiceLoader<>( aggregatedClassLoader, serviceContract );
		}
		else {
			// Java 8
			return new ClassPathOnlyAggregatedServiceLoader<>( aggregatedClassLoader, serviceContract );
		}
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
	 * An {@link AggregatedServiceLoader} that will only detect services defined in the class path,
	 * not in the module path,
	 * because it passes the aggregated classloader directly to the service loader.
	 * <p>
	 * This implementation is best when running Hibernate ORM on Java 8.
	 * On Java 9 and above, {@link ClassPathAndModulePathAggregatedServiceLoader} should be used.
	 *
	 * @param <S> The type of loaded services.
	 */
	private static class ClassPathOnlyAggregatedServiceLoader<S> extends AggregatedServiceLoader<S> {
		private final ServiceLoader<S> delegate;

		private ClassPathOnlyAggregatedServiceLoader(AggregatedClassLoader aggregatedClassLoader, Class<S> serviceContract) {
			this.delegate = ServiceLoader.load( serviceContract, aggregatedClassLoader );
		}

		@Override
		public Collection<S> getAll() {
			final Set<S> services = new LinkedHashSet<>();
			for ( S service : delegate ) {
				services.add( service );
			}
			return services;
		}

		@Override
		public void close() {
			// Clear service providers
			delegate.reload();
		}
	}

	/**
	 * An {@link AggregatedServiceLoader} that will detect services defined in the class path or in the module path.
	 * <p>
	 * This implementation only works when running Hibernate ORM on Java 9 and above.
	 * On Java 8, {@link ClassPathOnlyAggregatedServiceLoader} must be used.
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
				 * in ClassPathOnlyAggregatedServiceLoader.getAll().
				 */
				cache = Collections.unmodifiableCollection( loadAll() );
			}
			return cache;
		}

		private Collection<S> loadAll() {
			Set<String> alreadyEncountered = new HashSet<>();
			Set<S> result = new LinkedHashSet<>();

			// Always try the aggregated class loader first
			Iterator<? extends Supplier<S>> providerIterator = providerStream( aggregatedClassLoaderServiceLoader )
					.iterator();
			while ( providerIterator.hasNext() ) {
				Supplier<S> provider = providerIterator.next();
				collectServiceIfNotDuplicate( result, alreadyEncountered, provider );
			}

			/*
			 * Then also try the individual class loaders,
			 * because only them can instantiate services provided by jars in the module path.
			 */
			for ( ServiceLoader<S> delegate : delegates ) {
				providerIterator = providerStream( delegate ).iterator();
				/*
				 * Note that advancing the stream itself can lead to (arguably) "legitimate" errors,
				 * where we fail to load the service,
				 * but only because individual classloader has its own definition of the service contract class,
				 * which is different from ours.
				 * In that case (still arguably), the error should be ignored.
				 * That's why we wrap the call to hasNext in a method that catches an logs errors.
				 * See https://hibernate.atlassian.net/browse/HHH-13551.
				 */
				while ( hasNextIgnoringServiceConfigurationError( providerIterator ) ) {
					Supplier<S> provider = providerIterator.next();
					collectServiceIfNotDuplicate( result, alreadyEncountered, provider );
				}
			}

			return result;
		}

		@SuppressWarnings("unchecked")
		private Stream<? extends Supplier<S>> providerStream(ServiceLoader<S> serviceLoader) {
			try {
				return ( (Stream<? extends Supplier<S>>) SERVICE_LOADER_STREAM_METHOD.invoke( serviceLoader ) );
			}
			catch (RuntimeException | IllegalAccessException | InvocationTargetException e) {
				throw new AssertionFailure( "Error calling ServiceLoader.stream()", e );
			}
		}

		private boolean hasNextIgnoringServiceConfigurationError(Iterator<?> iterator) {
			while ( true ) {
				try {
					return iterator.hasNext();
				}
				catch (ServiceConfigurationError e) {
					log.ignoringServiceConfigurationError( serviceContract, e );
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
		private void collectServiceIfNotDuplicate(Set<S> result, Set<String> alreadyEncountered, Supplier<S> provider) {
			Class<?> type;
			try {
				type = (Class<?>) PROVIDER_TYPE_METHOD.invoke( provider );
			}
			catch (RuntimeException | IllegalAccessException | InvocationTargetException e) {
				throw new AssertionFailure( "Error calling ServiceLoader.Provider.type()", e );
			}
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
