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

import java.util.Dictionary;
import java.util.Hashtable;

import javax.persistence.spi.PersistenceProvider;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.internal.util.ClassLoaderHelper;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 * This BundleActivator provides three different uses of Hibernate in OSGi
 * environments:
 * 
 * 1.) Enterprise OSGi JPA: The OSGi container/runtime is in charge of managing
 *     all of the client bundles' persistence units.  The container/runtime is
 *     also in charge of creating and managing the EntityManagerFactory through a
 *     registered PersistenceProvider (this).
 * 2.) Un-managed OSGI JPA: Same as #1, but the container does not manage
 *     the persistence units.  Client bundles identify a typical
 *     PersistenceProvider, registered by this activator.
 * 3.) Client bundles create and manage their own SessionFactory.  A
 *     SessionFactory is registered as an OSGi ServiceFactory -- each requesting
 *     bundle gets its own instance of a SessionFactory.  The use of services,
 *     rather than direct use of Configuration, is necessary to shield users
 *     from ClassLoader issues.  See {@link OsgiSessionFactoryService} for more
 *     information.
 * 
 * @author Brett Meyer
 * @author Tim Ward
 */
@SuppressWarnings("UnusedDeclaration")
public class HibernateBundleActivator implements BundleActivator {
	
	private OsgiClassLoader osgiClassLoader;
	private OsgiServiceUtil osgiServiceUtil;
	
	private ServiceRegistration<?> persistenceProviderService;
	private ServiceRegistration<?> sessionFactoryService;
	
	@Override
	@SuppressWarnings("unchecked")
	public void start(BundleContext context) throws Exception {
		// build a ClassLoader that uses all the necessary OSGi bundles, and place it into
		// a well-known location so internals can access it
		osgiClassLoader = new OsgiClassLoader();
		osgiClassLoader.addBundle( FrameworkUtil.getBundle( Session.class ) );
		osgiClassLoader.addBundle( FrameworkUtil.getBundle( HibernatePersistenceProvider.class ) );
		ClassLoaderHelper.overridenClassLoader = osgiClassLoader;
		
		osgiServiceUtil = new OsgiServiceUtil( context );

		// Build a JtaPlatform specific for this OSGi context
		final OsgiJtaPlatform osgiJtaPlatform = new OsgiJtaPlatform( osgiServiceUtil );

		final Dictionary properties = new Hashtable();
		// In order to support existing persistence.xml files, register using the legacy provider name.
		properties.put( "javax.persistence.provider", HibernatePersistenceProvider.class.getName() );
		persistenceProviderService = context.registerService(
				PersistenceProvider.class.getName(),
				new OsgiPersistenceProviderService( osgiClassLoader, osgiJtaPlatform, osgiServiceUtil ),
				properties
		);
		sessionFactoryService = context.registerService(
				SessionFactory.class.getName(),
				new OsgiSessionFactoryService( osgiClassLoader, osgiJtaPlatform, osgiServiceUtil ),
				new Hashtable()
		);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		osgiClassLoader.stop();
		osgiClassLoader = null;
		osgiServiceUtil.stop();
		osgiServiceUtil = null;
		
		persistenceProviderService.unregister();
		persistenceProviderService = null;
		sessionFactoryService.unregister();
		sessionFactoryService = null;

		ClassLoaderHelper.overridenClassLoader = null;
	}
}
