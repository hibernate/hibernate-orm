/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import javax.persistence.spi.PersistenceProvider;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceProvider;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
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
 *     rather than manual building of the SessionFactory, is necessary to shield users
 *     from ClassLoader issues.  See {@link OsgiSessionFactoryService} for more
 *     information.
 * 
 * @author Brett Meyer
 * @author Tim Ward
 */
@SuppressWarnings("UnusedDeclaration")
public class HibernateBundleActivator implements BundleActivator {
	private OsgiServiceUtil osgiServiceUtil;
	
	private ServiceRegistration<?> persistenceProviderService;
	private ServiceRegistration<?> sessionFactoryService;
	
	@Override
	@SuppressWarnings("unchecked")
	public void start(BundleContext context) throws Exception {
		osgiServiceUtil = new OsgiServiceUtil( context );

		// Build a JtaPlatform specific for this OSGi context
		final OsgiJtaPlatform osgiJtaPlatform = new OsgiJtaPlatform( osgiServiceUtil );

		final Dictionary properties = new Hashtable();
		// In order to support existing persistence.xml files, register using the legacy provider name.
		properties.put( "javax.persistence.provider", HibernatePersistenceProvider.class.getName() );
		persistenceProviderService = context.registerService(
				PersistenceProvider.class.getName(),
				new OsgiPersistenceProviderService( osgiJtaPlatform, osgiServiceUtil ),
				properties
		);
		sessionFactoryService = context.registerService(
				SessionFactory.class.getName(),
				new OsgiSessionFactoryService( osgiJtaPlatform, osgiServiceUtil ),
				new Hashtable()
		);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		osgiServiceUtil.stop();
		osgiServiceUtil = null;
		
		persistenceProviderService.unregister();
		persistenceProviderService = null;
		sessionFactoryService.unregister();
		sessionFactoryService = null;
	}
}
