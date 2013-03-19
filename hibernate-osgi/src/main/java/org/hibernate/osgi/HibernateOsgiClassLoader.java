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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import org.hibernate.Session;
import org.hibernate.ejb.HibernatePersistence;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Custom OSGI ClassLoader helper which knows all the "interesting" bundles and
 * encapsulates the OSGi related capabilities.
 * 
 * @author Brett Meyer
 * @author Tim Ward
 */
public class HibernateOsgiClassLoader extends ClassLoader {

	private final Bundle hibernateCoreBundle = FrameworkUtil.getBundle( Session.class );
	private final Bundle hibernateEMBundle = FrameworkUtil.getBundle( HibernatePersistence.class );

	/**
	 * Load the class and break on first found match.
	 * TODO: Should this throw a different exception or warn if multiple
	 * classes were found? Naming collisions can and do happen in OSGi...
	 */
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> toReturn;
		try {
			toReturn = hibernateCoreBundle.loadClass( name );
		}
		catch ( ClassNotFoundException cnfe ) {
			toReturn = hibernateEMBundle.loadClass( name );
		}
		return toReturn;
	}

	/**
	 * Load the class and break on first found match.
	 * TODO: Should this throw a different exception or warn if multiple
	 * classes were found? Naming collisions can and do happen in OSGi...
	 */
	@Override
	protected URL findResource(String name) {
		URL toReturn = hibernateCoreBundle.getResource( name );
		if ( toReturn == null ) {
			toReturn = hibernateEMBundle.getResource( name );
		}
		return toReturn;
	}

	/**
	 * Load the class and break on first found match.
	 * TODO: Should this throw a different exception or warn if multiple
	 * classes were found? Naming collisions can and do happen in OSGi...
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		final Enumeration<URL> a = hibernateCoreBundle.getResources( name );
		final Enumeration<URL> b = hibernateEMBundle.getResources( name );
		return new Enumeration<URL>() {

			@Override
			public boolean hasMoreElements() {
				return ( a != null && a.hasMoreElements() ) || ( b != null && b.hasMoreElements() );
			}

			@Override
			public URL nextElement() {
				if ( a != null && a.hasMoreElements() ) {
					return a.nextElement();
				}
				else if ( b != null && b.hasMoreElements() ) {
					return b.nextElement();
				}
				throw new NoSuchElementException();
			}
		};
	}
}
