/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * See the description on {@link OsgiSessionFactoryService}.  This class is similar, providing an
 * PersistenceProvider as an OSGi Service.
 * 
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiPersistenceProviderService implements ServiceFactory {
	private OsgiJtaPlatform osgiJtaPlatform;
	private OsgiServiceUtil osgiServiceUtil;

	/**
	 * Constructs a OsgiPersistenceProviderService
	 *
	 * @param osgiJtaPlatform The OSGi-specific JtaPlatform created in HibernateBundleActivator
	 */
	public OsgiPersistenceProviderService(
			OsgiJtaPlatform osgiJtaPlatform,
			OsgiServiceUtil osgiServiceUtil) {
		this.osgiJtaPlatform = osgiJtaPlatform;
		this.osgiServiceUtil = osgiServiceUtil;
	}

	@Override
	public Object getService(Bundle requestingBundle, ServiceRegistration registration) {
		final OsgiClassLoader osgiClassLoader = new OsgiClassLoader();

		// First, add the client bundle that's requesting the OSGi services.
		osgiClassLoader.addBundle( requestingBundle );

		// Then, automatically add hibernate-core and hibernate-entitymanager.  These are needed to load resources
		// contained in those jars, such as em's persistence.xml schemas.
		osgiClassLoader.addBundle( FrameworkUtil.getBundle( SessionFactory.class ) );
		osgiClassLoader.addBundle( FrameworkUtil.getBundle( HibernateEntityManagerFactory.class ) );

		// Some "boot time" code does still rely on TCCL.  "run time" code should all be using
		// ClassLoaderService now.

		final ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader( osgiClassLoader );
		try {
			return new OsgiPersistenceProvider( osgiClassLoader, osgiJtaPlatform, osgiServiceUtil, requestingBundle );
		}
		finally {
			Thread.currentThread().setContextClassLoader( originalTccl );
		}
	}

	@Override
	public void ungetService(Bundle requestingBundle, ServiceRegistration registration, Object service) {
		// ?
	}

}
