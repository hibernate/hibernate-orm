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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.osgi.framework.Bundle;

/**
 * Custom OSGI ClassLoader helper which knows all the "interesting"
 * class loaders and bundles.  Encapsulates the OSGi related CL capabilities.
 * 
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiClassLoader extends ClassLoader {
	// Leave these as Sets -- addClassLoader or addBundle may be called more
	// than once if a SF or EMF is closed and re-created.
	private Set<ClassLoader> classLoaders = new HashSet<ClassLoader>();
	private Set<Bundle> bundles = new HashSet<Bundle>();

	private Map<String, Class<?>> classCache = new HashMap<String, Class<?>>();
	
	private Map<String, URL> resourceCache = new HashMap<String, URL>();

	/**
	 * Load the class and break on first found match.
	 * TODO: Should this throw a different exception or warn if multiple
	 * classes were found? Naming collisions can and do happen in OSGi...
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if ( classCache.containsKey( name ) ) {
			return classCache.get( name );
		}
		
		for ( Bundle bundle : bundles ) {
			try {
				Class clazz = bundle.loadClass( name );
				if ( clazz != null ) {
					classCache.put( name, clazz );
					return clazz;
				}
			}
			catch ( Exception ignore ) {
			}
		}
		
		for ( ClassLoader classLoader : classLoaders ) {
			try {
				Class clazz = classLoader.loadClass( name );
				if ( clazz != null ) {
					classCache.put( name, clazz );
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
		if ( resourceCache.containsKey( name ) ) {
			return resourceCache.get( name );
		}
		
		for ( Bundle bundle : bundles ) {
			try {
				URL resource = bundle.getResource( name );
				if ( resource != null ) {
					resourceCache.put( name, resource );
					return resource;
				}
			}
			catch ( Exception ignore ) {
			}
		}
		
		for ( ClassLoader classLoader : classLoaders ) {
			try {
				URL resource = classLoader.getResource( name );
				if ( resource != null ) {
					resourceCache.put( name, resource );
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
	 * 
	 * Note: Since they're Enumerations, do not cache these results!  
	 * 
	 * TODO: Should this throw a different exception or warn if multiple
	 * classes were found? Naming collisions can and do happen in OSGi...
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Enumeration<URL> findResources(String name) {
		final List<Enumeration<URL>> enumerations = new ArrayList<Enumeration<URL>>();
		
		for ( Bundle bundle : bundles ) {
			try {
				Enumeration<URL> resources = bundle.getResources( name );
				if ( resources != null ) {
					enumerations.add( resources );
				}
			}
			catch ( Exception ignore ) {
			}
		}
		
		for ( ClassLoader classLoader : classLoaders ) {
			try {
				Enumeration<URL> resources = classLoader.getResources( name );
				if ( resources != null ) {
					enumerations.add( resources );
				}
			}
			catch ( Exception ignore ) {
			}
		}
		
		Enumeration<URL> aggEnumeration = new Enumeration<URL>() {
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

	public void addClassLoader( ClassLoader classLoader ) {
		classLoaders.add( classLoader );
	}

	public void addBundle( Bundle bundle ) {
		bundles.add( bundle );
	}
	
	public void clear() {
		classCache.clear();
		resourceCache.clear();
	}

}
