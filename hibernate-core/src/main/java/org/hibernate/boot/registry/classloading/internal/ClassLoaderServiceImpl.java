/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.classloading.internal;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;

import static java.util.Collections.singletonList;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;

/**
 * Standard implementation of the service for interacting with class loaders
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public class ClassLoaderServiceImpl implements ClassLoaderService {

	private static final String CLASS_PATH_SCHEME = "classpath://";

	private final ConcurrentMap<Class<?>, AggregatedServiceLoader<?>> serviceLoaders = new ConcurrentHashMap<>();
	private volatile AggregatedClassLoader aggregatedClassLoader;

	/**
	 * Constructs a ClassLoaderServiceImpl with standard set-up
	 */
	public ClassLoaderServiceImpl() {
		this( ClassLoaderServiceImpl.class.getClassLoader() );
	}

	/**
	 * Constructs a ClassLoaderServiceImpl with the given ClassLoader
	 *
	 * @param classLoader The ClassLoader to use
	 */
	public ClassLoaderServiceImpl(@Nonnull ClassLoader classLoader) {
		this( singletonList( classLoader ),TcclLookupPrecedence.AFTER );
	}

	/**
	 * Constructs a ClassLoaderServiceImpl with the given ClassLoader instances
	 *
	 * @param providedClassLoaders The ClassLoader instances to use
	 * @param lookupPrecedence The lookup precedence of the thread context {@code ClassLoader}
	 */
	public ClassLoaderServiceImpl(
			@Nullable Collection<ClassLoader> providedClassLoaders,
			@Nonnull TcclLookupPrecedence lookupPrecedence) {
		final LinkedHashSet<ClassLoader> orderedClassLoaderSet = new LinkedHashSet<>();

		// first, add all provided class loaders, if any
		if ( providedClassLoaders != null ) {
			for ( ClassLoader classLoader : providedClassLoaders ) {
				if ( classLoader != null ) {
					orderedClassLoaderSet.add( classLoader );
				}
			}
		}

		// normalize adding known class-loaders...
		// then the Hibernate class loader
		orderedClassLoaderSet.add( ClassLoaderServiceImpl.class.getClassLoader() );

		// now build the aggregated class loader...
		this.aggregatedClassLoader = new AggregatedClassLoader( orderedClassLoaderSet, lookupPrecedence );
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public <T> Class<T> classForName(@Nonnull String className) {
		try {
			return (Class<T>) Class.forName( className, true, getAggregatedClassLoader() );
		}
		catch (Exception | LinkageError e) {
			throw new ClassLoadingException( "Unable to load class [" + className + "]", e );
		}
	}

	@Override
	@Nullable
	public URL locateResource(@Nonnull String name) {
		// first we try name as a URL
		try {
			return new URL( name );
		}
		catch (Exception ignore) {
		}

		// if we couldn't find the resource containing a classpath:// prefix above, that means we don't have a URL
		// handler for it. So let's remove the prefix and resolve against our class loader.
		name = stripClasspathScheme( name );

		try {
			final URL url = getAggregatedClassLoader().getResource( name );
			if ( url != null ) {
				return url;
			}
		}
		catch (Exception ignore) {
		}

		if ( name.startsWith( "/" ) ) {
			name = name.substring( 1 );

			try {
				final URL url = getAggregatedClassLoader().getResource( name );
				if ( url != null ) {
					return url;
				}
			}
			catch (Exception ignore) {
			}
		}

		return null;
	}

	@Override
	@Nullable
	public InputStream locateResourceStream(@Nonnull String name) {
		// first we try name as a URL
		try {
			BOOT_LOGGER.tryingURL( name );
			return new URL( name ).openStream();
		}
		catch (Exception ignore) {
		}

		// if we couldn't find the resource containing a classpath:// prefix above, that means we don't have a URL
		// handler for it. So let's remove the prefix and resolve against our class loader.
		name = stripClasspathScheme( name );

		try {
			BOOT_LOGGER.tryingClassLoader( name );
			final InputStream stream = getAggregatedClassLoader().getResourceAsStream( name );
			if ( stream != null ) {
				return stream;
			}
		}
		catch (Exception ignore) {
		}

		final String stripped = name.startsWith( "/" ) ? name.substring( 1 ) : null;

		if ( stripped != null ) {
			try {
				BOOT_LOGGER.tryingURL( stripped );
				return new URL( stripped ).openStream();
			}
			catch (Exception ignore) {
			}

			try {
				BOOT_LOGGER.tryingClassLoader( stripped );
				final InputStream stream = getAggregatedClassLoader().getResourceAsStream( stripped );
				if ( stream != null ) {
					return stream;
				}
			}
			catch (Exception ignore) {
			}
		}

		return null;
	}

	@Override
	@Nonnull
	public List<URL> locateResources(@Nonnull String name) {
		final ArrayList<URL> urls = new ArrayList<>();
		try {
			final Enumeration<URL> urlEnumeration = getAggregatedClassLoader().getResources( name );
			if ( urlEnumeration != null && urlEnumeration.hasMoreElements() ) {
				while ( urlEnumeration.hasMoreElements() ) {
					urls.add( urlEnumeration.nextElement() );
				}
			}
		}
		catch (Exception ignore) {
		}

		return urls;
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <S> Collection<S> loadJavaServices(@Nonnull Class<S> serviceContract) {
		var serviceLoader = (AggregatedServiceLoader<S>) serviceLoaders.get( serviceContract );
		if ( serviceLoader == null ) {
			serviceLoader = AggregatedServiceLoader.create( getAggregatedClassLoader(), serviceContract );
			serviceLoaders.put( serviceContract, serviceLoader );
		}
		return serviceLoader.getAll();
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public <T> T generateProxy(@Nonnull InvocationHandler handler, @Nonnull Class<?>... interfaces) {
		return (T) Proxy.newProxyInstance(
				getAggregatedClassLoader(),
				interfaces,
				handler
		);
	}

	@Override
	@Nullable
	public Package packageForNameOrNull(@Nonnull String packageName) {
		try {
			return Class.forName( packageName + ".package-info", true, getAggregatedClassLoader() )
					.getPackage();
		}
		catch (ClassNotFoundException e) {
			BOOT_LOGGER.packageNotFound( packageName );
			return null;
		}
		catch (LinkageError e) {
			BOOT_LOGGER.linkageError( packageName, e );
			return null;
		}
	}

	@Override
	public <T> T workWithClassLoader(@Nonnull Work<T> work) {
		return work.doWork( getAggregatedClassLoader() );
	}

	private AggregatedClassLoader getAggregatedClassLoader() {
		final AggregatedClassLoader aggregated = this.aggregatedClassLoader;
		if ( aggregated == null ) {
			throw new HibernateException( "The ClassLoaderService cannot be reused (this instance was stopped already)" );
		}
		return aggregated;
	}

	private String stripClasspathScheme(String name) {
		if ( name == null ) {
			return null;
		}

		if ( name.startsWith( CLASS_PATH_SCHEME ) ) {
			return name.substring( CLASS_PATH_SCHEME.length() );
		}

		return name;
	}

	@Override
	public void stop() {
		for ( AggregatedServiceLoader<?> serviceLoader : serviceLoaders.values() ) {
			serviceLoader.close();
		}
		serviceLoaders.clear();
		//Avoid ClassLoader leaks
		this.aggregatedClassLoader = null;
	}

}
