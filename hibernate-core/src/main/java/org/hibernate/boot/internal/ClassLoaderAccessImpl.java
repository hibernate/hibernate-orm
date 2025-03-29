/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	private final ClassLoaderService classLoaderService;
	private ClassLoader jpaTempClassLoader;

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

	public void injectTempClassLoader(ClassLoader jpaTempClassLoader) {
		log.debugf( "ClassLoaderAccessImpl#injectTempClassLoader(%s) [was %s]", jpaTempClassLoader, this.jpaTempClassLoader );
		this.jpaTempClassLoader = jpaTempClassLoader;
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
			|| name.startsWith( "jakarta." )
			|| name.startsWith( "org.hibernate." );

	}

	public ClassLoader getJpaTempClassLoader() {
		return jpaTempClassLoader;
	}

	@Override
	public URL locateResource(String resourceName) {
		return classLoaderService.locateResource( resourceName );
	}

	public void release() {
	}
}
