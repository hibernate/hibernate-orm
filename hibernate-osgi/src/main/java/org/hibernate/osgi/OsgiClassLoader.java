/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.osgi;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;

import org.osgi.framework.Bundle;

/**
 * Custom OSGI ClassLoader helper which knows all the "interesting" bundles and
 * encapsulates the OSGi related capabilities.
 * 
 * @author Brett Meyer
 */
public class OsgiClassLoader extends ClassLoader {

	private HashMap<String, Bundle> bundles;

	public OsgiClassLoader() {
		bundles = new HashMap<String, Bundle>();
	}

	/**
	 * Load the class and break on first found match.
	 * TODO: Should this throw a different exception or warn if multiple
	 * classes were found? Naming collisions can and do happen in OSGi...
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		// TODO: This is horrible -- we shouldn't iterate over all the
		// classloaders every time we need to construct an entity, etc. Instead,
		// keep references to all classes/resources found in active bundles
		// in memory? Find a way to identify what we "care about" and keep
		// only those? Discover them the first time and then cache the
		// reference?
		for ( Bundle bundle : bundles.values() ) {
			try {
				Class clazz = bundle.loadClass( name );
				if ( clazz != null ) {
					return clazz;
				}
			}
			catch ( Exception ignore ) {
			}
		}

		throw new ClassNotFoundException( "Could not load requested class : " + name );
	}

	/**
	 * Load the class and break on first found match.
	 * TODO: Should this throw a different exception or warn if multiple
	 * classes were found? Naming collisions can and do happen in OSGi...
	 */
	@Override
	protected URL findResource(String name) {
		// TODO: This is horrible -- we shouldn't iterate over all the
		// classloaders every time we need to construct an entity, etc. Instead,
		// keep references to all classes/resources found in active bundles
		// in memory? Find a way to identify what we "care about" and keep
		// only those? Discover them the first time and then cache the
		// reference?
		for ( Bundle bundle : bundles.values() ) {
			try {
				URL resource = bundle.getResource( name );
				if ( resource != null ) {
					return resource;
				}
			}
			catch ( Exception ignore ) {
			}
		}
		// TODO: Error?
		return null;
	}

	/**
	 * Load the class and break on first found match.
	 * TODO: Should this throw a different exception or warn if multiple
	 * classes were found? Naming collisions can and do happen in OSGi...
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Enumeration<URL> findResources(String name) {
		// TODO: This is horrible -- we shouldn't iterate over all the
		// classloaders every time we need to construct an entity, etc. Instead,
		// keep references to all classes/resources found in active bundles
		// in memory? Find a way to identify what we "care about" and keep
		// only those? Discover them the first time and then cache the
		// reference?
		for ( Bundle bundle : bundles.values() ) {
			try {
				Enumeration<URL> resources = bundle.getResources( name );
				if ( resources != null ) {
					return resources;
				}
			}
			catch ( Exception ignore ) {
			}
		}
		// TODO: Error?
		return null;
	}

	/**
	 * Register the bundle with this class loader
	 */
	public void registerBundle(Bundle bundle) {
		if ( bundle != null ) {
			synchronized ( bundles ) {
				// create a bundle classloader and add it to the list of
				// classloaders
				String key = getBundleKey( bundle );
				if ( !bundles.containsKey( key ) ) {
					bundles.put( key, bundle );
				}
			}
		}
	}

	/**
	 * Unregister the bundle from this class loader
	 */
	public void unregisterBundle(Bundle bundle) {
		if ( bundle != null ) {
			synchronized ( bundles ) {
				// remove a bundle classloader for a given bundle
				String key = getBundleKey( bundle );
				if ( bundles.containsKey( key ) ) {
					bundles.remove( key );
				}
			}
		}
	}
	
	public void clear() {
		bundles.clear();
	}

	protected static String getBundleKey(Bundle bundle) {
		return bundle.getSymbolicName() + " " + bundle.getVersion().toString();
	}

}
