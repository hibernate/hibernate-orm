/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi;

import org.osgi.framework.Bundle;
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
	private OsgiClassLoader osgiClassLoader;
	private OsgiJtaPlatform osgiJtaPlatform;
	private OsgiServiceUtil osgiServiceUtil;

	/**
	 * Constructs a OsgiPersistenceProviderService
	 *
	 * @param osgiClassLoader The OSGi-specific ClassLoader created in HibernateBundleActivator
	 * @param osgiJtaPlatform The OSGi-specific JtaPlatform created in HibernateBundleActivator
	 * @param context The OSGi context
	 */
	public OsgiPersistenceProviderService(
			OsgiClassLoader osgiClassLoader,
			OsgiJtaPlatform osgiJtaPlatform,
			OsgiServiceUtil osgiServiceUtil) {
		this.osgiClassLoader = osgiClassLoader;
		this.osgiJtaPlatform = osgiJtaPlatform;
		this.osgiServiceUtil = osgiServiceUtil;
	}

	@Override
	public Object getService(Bundle requestingBundle, ServiceRegistration registration) {
		return new OsgiPersistenceProvider( osgiClassLoader, osgiJtaPlatform, osgiServiceUtil, requestingBundle );
	}

	@Override
	public void ungetService(Bundle requestingBundle, ServiceRegistration registration, Object service) {
		// ?
	}

}
