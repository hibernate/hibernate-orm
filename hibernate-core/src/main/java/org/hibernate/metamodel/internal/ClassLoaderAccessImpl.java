/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal;

import java.net.URL;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.metamodel.spi.ClassLoaderAccess;
import org.hibernate.service.ServiceRegistry;
import org.jboss.logging.Logger;

/**
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

	@Override
	@SuppressWarnings("unchecked")
	public Class<?> classForName(String name) {
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
				|| name.startsWith( "org.hibernate" );

	}

	@Override
	public URL locateResource(String resourceName) {
		return classLoaderService.locateResource( resourceName );
	}
}
