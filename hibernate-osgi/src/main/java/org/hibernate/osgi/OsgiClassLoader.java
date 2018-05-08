/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.service.spi.Stoppable;
import org.osgi.framework.Bundle;

/**
 * Custom OSGI ClassLoader helper which knows all the "interesting"
 * class loaders and bundles.  Encapsulates the OSGi related CL capabilities.
 *
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiClassLoader extends ClassLoader implements Stoppable {
	// Leave these as Sets -- addClassLoader or addBundle may be called more
	// than once if a SF or EMF is closed and re-created.
	// HHH-12553: must be thread-safe. Concurrent impl. would be best, but we have to retain insertion-order.
	private Set<ClassLoader> classLoaders = Collections.synchronizedSet(new LinkedHashSet<ClassLoader>());
	private Set<Bundle> bundles = Collections.synchronizedSet(new LinkedHashSet<Bundle>());

	private ConcurrentMap<String, Class<?>> classCache = new ConcurrentHashMap<String, Class<?>>();
	private ConcurrentMap<String, URL> resourceCache = new ConcurrentHashMap<String, URL>();

	static {
		ClassLoader.registerAsParallelCapable();
	}

	public OsgiClassLoader() {
		// DO NOT use ClassLoader#parent, which is typically the SystemClassLoader for most containers.  Instead,
		// allow the ClassNotFoundException to be thrown.  ClassLoaderServiceImpl will check the SystemClassLoader
		// later on.  This is especially important for embedded OSGi containers, etc.
		super( null );
	}

	/**
	 * Load the class and break on first found match.
	 *
	 * TODO: Should this throw a different exception or warn if multiple
	 * classes were found? Naming collisions can and do happen in OSGi...
	 */
	@Override
	@SuppressWarnings("rawtypes")
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class< ? > cachedClass = classCache.get( name );
		if ( cachedClass != null ) {
			return cachedClass;
		}

		synchronized (bundles) {
			for ( Bundle bundle : bundles ) {
				try {
					final Class clazz = bundle.loadClass( name );
					if ( clazz != null ) {
						classCache.put( name, clazz );
						return clazz;
					}
				}
				catch ( Exception ignore ) {
				}
			}
		}

		synchronized (classLoaders) {
			for ( ClassLoader classLoader : classLoaders ) {
				try {
					final Class clazz = classLoader.loadClass( name );
					if ( clazz != null ) {
						classCache.put( name, clazz );
						return clazz;
					}
				}
				catch ( Exception ignore ) {
				}
			}
		}

		throw new ClassNotFoundException( "Could not load requested class : " + name );
	}

	/**
	 * Load the class and break on first found match.
	 *
	 * TODO: Should this throw a different exception or warn if multiple
	 * classes were found? Naming collisions can and do happen in OSGi...
	 */
	@Override
	protected URL findResource(String name) {
		URL cachedResource = resourceCache.get( name );
		if ( cachedResource != null ) {
			return cachedResource;
		}

		synchronized (bundles) {
			for ( Bundle bundle : bundles ) {
				try {
					final URL resource = bundle.getResource( name );
					if ( resource != null ) {
						resourceCache.put( name, resource );
						return resource;
					}
				}
				catch ( Exception ignore ) {
				}
			}
		}

		synchronized (classLoaders) {
			for ( ClassLoader classLoader : classLoaders ) {
				try {
					final URL resource = classLoader.getResource( name );
					if ( resource != null ) {
						resourceCache.put( name, resource );
						return resource;
					}
				}
				catch ( Exception ignore ) {
				}
			}
		}

		// TODO: Error?
		return null;
	}

	/**
	 * Load the class and break on first found match.
	 *
	 * Note: Since they're Enumerations, do not cache these results!
	 *
	 * TODO: Should this throw a different exception or warn if multiple
	 * classes were found? Naming collisions can and do happen in OSGi...
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Enumeration<URL> findResources(String name) {
		final List<Enumeration<URL>> enumerations = new ArrayList<Enumeration<URL>>();

		synchronized (bundles) {
			for ( Bundle bundle : bundles ) {
				try {
					final Enumeration<URL> resources = bundle.getResources( name );
					if ( resources != null ) {
						enumerations.add( resources );
					}
				}
				catch ( Exception ignore ) {
				}
			}
		}

		synchronized (classLoaders) {
			for ( ClassLoader classLoader : classLoaders ) {
				try {
					final Enumeration<URL> resources = classLoader.getResources( name );
					if ( resources != null ) {
						enumerations.add( resources );
					}
				}
				catch ( Exception ignore ) {
				}
			}
		}

		final Enumeration<URL> aggEnumeration = new Enumeration<URL>() {
			@Override
			public boolean hasMoreElements() {
				for ( Enumeration<URL> enumeration : enumerations ) {
					if ( enumeration != null && enumeration.hasMoreElements() ) {
						return true;
					}
				}
				return false;
			}

			@Override
			public URL nextElement() {
				for ( Enumeration<URL> enumeration : enumerations ) {
					if ( enumeration != null && enumeration.hasMoreElements() ) {
						return enumeration.nextElement();
					}
				}
				throw new NoSuchElementException();
			}
		};

		return aggEnumeration;
	}

	/**
	 * Adds a ClassLoader to the wrapped set of ClassLoaders
	 *
	 * @param classLoader The ClassLoader to add
	 */
	public void addClassLoader( ClassLoader classLoader ) {
		classLoaders.add( classLoader );
	}

	/**
	 * Adds a Bundle to the wrapped set of Bundles
	 *
	 * @param bundle The Bundle to add
	 */
	public void addBundle( Bundle bundle ) {
		bundles.add( bundle );
	}

	@Override
	public void stop() {
		classLoaders.clear();
		bundles.clear();
		classCache.clear();
		resourceCache.clear();
	}

}
