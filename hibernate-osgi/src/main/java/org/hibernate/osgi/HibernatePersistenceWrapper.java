/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.hibernate.ejb.HibernatePersistence;
import org.osgi.framework.Bundle;

/**
 * <p>
 * A simple wrapper that sets the relevant classloader properties to ensure that hibernate knows how to access the
 * various classes it's going to need.
 * </p>
 * <p>
 * Taken from <a href="http://sourceforge.net/apps/mediawiki/hibernate/index.php?title=ClassLoaderService">
 * http://sourceforge.net/apps/mediawiki/hibernate/index.php?title=ClassLoaderService </a>
 * </p>
 * 
 * <pre>
 *      hibernate.classLoader.hibernate - The class loader for Hibernate classes, the default is to use the class loader of the org.hibernate.engine.basic.spi.ClassLoaderService Class.
 *      hibernate.classLoader.application - The java.lang.ClassLoader for application classes, the default is to use:
 *         The thread context class loader
 *         The Hibernate class loader 
 *      hibernate.classLoader.environment - The environment class loader. The only real anticipate use for this is to identify the class loader for JDBC classes.
 *      hibernate.classLoader.resources - The class loader for resource lookups. The default is to use the application class loader.
 * </pre>
 * 
 * 
 * @author Tim Ward
 */
public class HibernatePersistenceWrapper implements PersistenceProvider {

	private static final String HIBERNATE_CLASS_LOADER_RESOURCES = "hibernate.classLoader.resources";
	private static final String HIBERNATE_CLASS_LOADER_ENVIRONMENT = "hibernate.classLoader.environment";
	private static final String HIBERNATE_CLASS_LOADER_APPLICATION = "hibernate.classLoader.application";
	private static final String HIBERNATE_CLASS_LOADER_HIBERNATE = "hibernate.classLoader.hibernate";

	/** The delegate */
	private final HibernatePersistence hp;
	/** A ClassLoader that unifies the necessary hibernate bundles (core and entitymanager) */
	private final ClassLoader hibernateClassLoader;

	public HibernatePersistenceWrapper(HibernatePersistence hp, ClassLoader hibernateClassLoader) {
		this.hp = hp;
		this.hibernateClassLoader = hibernateClassLoader;
	}

	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {

		properties.put( HIBERNATE_CLASS_LOADER_HIBERNATE, hibernateClassLoader );
		properties.put( HIBERNATE_CLASS_LOADER_APPLICATION, info.getClassLoader() );
		properties.put( HIBERNATE_CLASS_LOADER_ENVIRONMENT, info.getClassLoader() );
		properties.put( HIBERNATE_CLASS_LOADER_RESOURCES, info.getClassLoader() );
		return hp.createContainerEntityManagerFactory( info, properties );
	}

	@Override
	public EntityManagerFactory createEntityManagerFactory(String name, Map properties) {
		final Bundle b = (Bundle) properties.get( "hibernate.osgi.persistence.bundle" );
		if ( b == null ) {
			throw new NullPointerException( "No persistence bundle was set in the properties for the persistence unit "
					+ name );
		}
		ClassLoader appClassLoader = new ClassLoader() {

			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				return b.loadClass( name );
			}

			@Override
			protected URL findResource(String name) {
				return b.getResource( name );
			}

			@Override
			protected Enumeration<URL> findResources(String name) throws IOException {
				return b.getResources( name );
			}
		};

		properties.put( HIBERNATE_CLASS_LOADER_HIBERNATE, hibernateClassLoader );
		properties.put( HIBERNATE_CLASS_LOADER_APPLICATION, appClassLoader );
		properties.put( HIBERNATE_CLASS_LOADER_ENVIRONMENT, appClassLoader );
		properties.put( HIBERNATE_CLASS_LOADER_RESOURCES, appClassLoader );
		return hp.createEntityManagerFactory( name, properties );
	}

	@Override
	public ProviderUtil getProviderUtil() {
		return hp.getProviderUtil();
	}
}
