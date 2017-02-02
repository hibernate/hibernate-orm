/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.net.URL;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.service.ServiceRegistry;

import org.jboss.logging.Logger;

/**
 * Standard implementation of ClassLoaderAccess
 *
 * @author Steve Ebersole
 */
public class ClassLoaderAccessImpl implements ClassLoaderAccess {
	private static final Logger log = Logger.getLogger( ClassLoaderAccessImpl.class );

	private final ClassLoader jpaTempClassLoader;
	private final ClassLoaderService classLoaderService;

	public ClassLoaderAccessImpl(
			ClassLoader jpaTempClassLoader,
			ClassLoaderService classLoaderService) {
		this.jpaTempClassLoader = jpaTempClassLoader;
		this.classLoaderService = classLoaderService;
	}

	public ClassLoaderAccessImpl(ClassLoader tempClassLoader, ServiceRegistry serviceRegistry) {
		this( tempClassLoader, serviceRegistry.getService( ClassLoaderService.class ) );
	}

	public ClassLoaderAccessImpl(ClassLoaderService classLoaderService) {
		this( null, classLoaderService );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<?> classForName(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "Name of class to load cannot be null" );
		}

		if ( isSafeClass( name ) ) {
			return classLoaderService.classForName( name );
		}
		else {
			log.debugf( "Not known whether passed class name [%s] is safe", name );
			if ( jpaTempClassLoader == null ) {
				log.debugf(
						"No temp ClassLoader provided; using live ClassLoader " +
								"for loading potentially unsafe class : %s",
						name
				);
				return classLoaderService.classForName( name );
			}
			else {
				log.debugf(
						"Temp ClassLoader was provided, so we will use that : %s",
						name
				);
				try {
					return jpaTempClassLoader.loadClass( name );
				}
				catch (ClassNotFoundException e) {
					throw new ClassLoadingException( name );
				}
			}
		}
	}

	private boolean isSafeClass(String name) {
		// classes in any of these packages are safe to load through the "live" ClassLoader
		return name.startsWith( "java." )
				|| name.startsWith( "javax." )
				|| name.startsWith( "org.hibernate." );

	}

	@Override
	public URL locateResource(String resourceName) {
		return classLoaderService.locateResource( resourceName );
	}
}
