/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.classloading.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Standard implementation of the service for interacting with class loaders
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public class ClassLoaderServiceImpl implements ClassLoaderService {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( ClassLoaderServiceImpl.class );

	private final ConcurrentMap<Class, ServiceLoader> serviceLoaders = new ConcurrentHashMap<Class, ServiceLoader>();
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
	public ClassLoaderServiceImpl(ClassLoader classLoader) {
		this( Collections.singletonList( classLoader ) );
	}

	/**
	 * Constructs a ClassLoaderServiceImpl with the given ClassLoader instances
	 *
	 * @param providedClassLoaders The ClassLoader instances to use
	 */
	public ClassLoaderServiceImpl(Collection<ClassLoader> providedClassLoaders) {
		final LinkedHashSet<ClassLoader> orderedClassLoaderSet = new LinkedHashSet<ClassLoader>();

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

		// then the TCCL, if one...
		final ClassLoader tccl = locateTCCL();
		if ( tccl != null ) {
			orderedClassLoaderSet.add( tccl );
		}
		// finally the system classloader
		final ClassLoader sysClassLoader = locateSystemClassLoader();
		if ( sysClassLoader != null ) {
			orderedClassLoaderSet.add( sysClassLoader );
		}

		// now build the aggregated class loader...
		this.aggregatedClassLoader = new AggregatedClassLoader( orderedClassLoaderSet );
	}

	/**
	 * No longer used/supported!
	 *
	 * @param configValues The config values
	 *
	 * @return The built service
	 *
	 * @deprecated No longer used/supported!
	 */
	@Deprecated
	@SuppressWarnings({"UnusedDeclaration", "unchecked", "deprecation"})
	public static ClassLoaderServiceImpl fromConfigSettings(Map configValues) {
		final List<ClassLoader> providedClassLoaders = new ArrayList<ClassLoader>();

		final Collection<ClassLoader> classLoaders = (Collection<ClassLoader>) configValues.get( AvailableSettings.CLASSLOADERS );
		if ( classLoaders != null ) {
			for ( ClassLoader classLoader : classLoaders ) {
				providedClassLoaders.add( classLoader );
			}
		}

		addIfSet( providedClassLoaders, AvailableSettings.APP_CLASSLOADER, configValues );
		addIfSet( providedClassLoaders, AvailableSettings.RESOURCES_CLASSLOADER, configValues );
		addIfSet( providedClassLoaders, AvailableSettings.HIBERNATE_CLASSLOADER, configValues );
		addIfSet( providedClassLoaders, AvailableSettings.ENVIRONMENT_CLASSLOADER, configValues );

		if ( providedClassLoaders.isEmpty() ) {
			log.debugf( "Incoming config yielded no classloaders; adding standard SE ones" );
			final ClassLoader tccl = locateTCCL();
			if ( tccl != null ) {
				providedClassLoaders.add( tccl );
			}
			providedClassLoaders.add( ClassLoaderServiceImpl.class.getClassLoader() );
		}

		return new ClassLoaderServiceImpl( providedClassLoaders );
	}

	private static void addIfSet(List<ClassLoader> providedClassLoaders, String name, Map configVales) {
		final ClassLoader providedClassLoader = (ClassLoader) configVales.get( name );
		if ( providedClassLoader != null ) {
			providedClassLoaders.add( providedClassLoader );
		}
	}

	private static ClassLoader locateSystemClassLoader() {
		try {
			return ClassLoader.getSystemClassLoader();
		}
		catch (Exception e) {
			return null;
		}
	}

	private static ClassLoader locateTCCL() {
		try {
			return Thread.currentThread().getContextClassLoader();
		}
		catch (Exception e) {
			return null;
		}
	}

	private static class AggregatedClassLoader extends ClassLoader {
		private final ClassLoader[] individualClassLoaders;

		private AggregatedClassLoader(final LinkedHashSet<ClassLoader> orderedClassLoaderSet) {
			super( null );
			individualClassLoaders = orderedClassLoaderSet.toArray( new ClassLoader[orderedClassLoaderSet.size()] );
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			final LinkedHashSet<URL> resourceUrls = new LinkedHashSet<URL>();

			for ( ClassLoader classLoader : individualClassLoaders ) {
				final Enumeration<URL> urls = classLoader.getResources( name );
				while ( urls.hasMoreElements() ) {
					resourceUrls.add( urls.nextElement() );
				}
			}

			return new Enumeration<URL>() {
				final Iterator<URL> resourceUrlIterator = resourceUrls.iterator();

				@Override
				public boolean hasMoreElements() {
					return resourceUrlIterator.hasNext();
				}

				@Override
				public URL nextElement() {
					return resourceUrlIterator.next();
				}
			};
		}

		@Override
		protected URL findResource(String name) {
			for ( ClassLoader classLoader : individualClassLoaders ) {
				final URL resource = classLoader.getResource( name );
				if ( resource != null ) {
					return resource;
				}
			}
			return super.findResource( name );
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			for ( ClassLoader classLoader : individualClassLoaders ) {
				try {
					return classLoader.loadClass( name );
				}
				catch (Exception ignore) {
				}
				catch (LinkageError ignore) {
				}
			}

			throw new ClassNotFoundException( "Could not load requested class : " + name );
		}

	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <T> Class<T> classForName(String className) {
		try {
			return (Class<T>) Class.forName( className, true, getAggregatedClassLoader() );
		}
		catch (Exception e) {
			throw new ClassLoadingException( "Unable to load class [" + className + "]", e );
		}
		catch (LinkageError e) {
			throw new ClassLoadingException( "Unable to load class [" + className + "]", e );
		}
	}

	@Override
	public URL locateResource(String name) {
		// first we try name as a URL
		try {
			return new URL( name );
		}
		catch (Exception ignore) {
		}

		try {
			return getAggregatedClassLoader().getResource( name );
		}
		catch (Exception ignore) {
		}

		return null;
	}

	@Override
	public InputStream locateResourceStream(String name) {
		// first we try name as a URL
		try {
			log.tracef( "trying via [new URL(\"%s\")]", name );
			return new URL( name ).openStream();
		}
		catch (Exception ignore) {
		}

		try {
			log.tracef( "trying via [ClassLoader.getResourceAsStream(\"%s\")]", name );
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
				log.tracef( "trying via [new URL(\"%s\")]", stripped );
				return new URL( stripped ).openStream();
			}
			catch (Exception ignore) {
			}

			try {
				log.tracef( "trying via [ClassLoader.getResourceAsStream(\"%s\")]", stripped );
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
	public List<URL> locateResources(String name) {
		final ArrayList<URL> urls = new ArrayList<URL>();
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

	@Override
	@SuppressWarnings("unchecked")
	public <S> Collection<S> loadJavaServices(Class<S> serviceContract) {
		ServiceLoader<S> serviceLoader = serviceLoaders.get( serviceContract );
		if ( serviceLoader == null ) {
			serviceLoader = ServiceLoader.load( serviceContract, getAggregatedClassLoader() );
			serviceLoaders.put( serviceContract, serviceLoader );
		}
		final LinkedHashSet<S> services = new LinkedHashSet<S>();
		for ( S service : serviceLoader ) {
			services.add( service );
		}
		return services;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T generateProxy(InvocationHandler handler, Class... interfaces) {
		return (T) Proxy.newProxyInstance(
				getAggregatedClassLoader(),
				interfaces,
				handler
		);
	}

	@Override
	public <T> T workWithClassLoader(Work<T> work) {
		return work.doWork( getAggregatedClassLoader() );
	}

	private ClassLoader getAggregatedClassLoader() {
		final ClassLoader aggregated = this.aggregatedClassLoader;
		if ( aggregated == null ) {
			throw log.usingStoppedClassLoaderService();
		}
		return aggregated;
	}

	@Override
	public void stop() {
		for ( ServiceLoader serviceLoader : serviceLoaders.values() ) {
			serviceLoader.reload(); // clear service loader providers
		}
		serviceLoaders.clear();
		//Avoid ClassLoader leaks
		this.aggregatedClassLoader = null;
	}

}
