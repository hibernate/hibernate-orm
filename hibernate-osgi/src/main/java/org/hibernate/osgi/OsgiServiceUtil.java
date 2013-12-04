/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.osgi;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.Stoppable;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utilities for dealing with OSGi environments
 * 
 * @author Brett Meyer
 */
public class OsgiServiceUtil implements Stoppable {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( OsgiServiceUtil.class );

	private BundleContext context;

	private Map<String, ServiceTracker> serviceTrackers = new HashMap<String, ServiceTracker>();

	public OsgiServiceUtil(BundleContext context) {
		this.context = context;
	}

	/**
	 * Locate all implementors of the given service contract in the given OSGi buindle context. Utilizes
	 * {@link ServiceTracker} (best practice, automatically handles a lot of boilerplate and error conditions).
	 * 
	 * @param contract The service contract for which to locate implementors
	 * @param context The OSGi bundle context
	 * @param T[] The Java type of the service to locate
	 * @return All know implementors
	 */
	public <T> T[] getServiceImpls(Class<T> contract) {
		T[] services = (T[]) Array.newInstance( contract, 0 );
		final ServiceTracker serviceTracker = getServiceTracker( contract.getName() );
		try {
			// Yep, this is stupid.  But, it's the only way to prevent #getServices from handing us back Object[].
			services = (T[]) serviceTracker.getServices( services );
			if ( services != null ) {
				return services;
			}
		}
		catch (Exception e) {
			LOG.unableToDiscoverOsgiService( contract.getName(), e );
		}
		return services;
	}

	/**
	 * Locate the single implementor of the given service contract in the given OSGi buindle context. Utilizes
	 * {@link ServiceTracker#waitForService(long)}
	 * 
	 * @param contract The service contract for which to locate implementors
	 * @param context The OSGi bundle context
	 * @param T[] The Java type of the service to locate
	 * @return All know implementors
	 */
	public <T> T getServiceImpl(Class<T> contract) {
		final ServiceTracker serviceTracker = getServiceTracker( contract.getName() );
		try {
			return (T) serviceTracker.waitForService( 1000 );
		}
		catch (Exception e) {
			LOG.unableToDiscoverOsgiService( contract.getName(), e );
			return null;
		}
	}

	private <T> ServiceTracker getServiceTracker(String contractClassName) {
		if ( !serviceTrackers.containsKey( contractClassName ) ) {
			final ServiceTracker<T, T> serviceTracker = new ServiceTracker<T, T>( context, contractClassName, null );
			serviceTracker.open();
			serviceTrackers.put( contractClassName, serviceTracker );
		}
		return serviceTrackers.get( contractClassName );
	}

	@Override
	public void stop() {
		for (String key : serviceTrackers.keySet()) {
			serviceTrackers.get( key ).close();
		}
		serviceTrackers.clear();
	}
}
