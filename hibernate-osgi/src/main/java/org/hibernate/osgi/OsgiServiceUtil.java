/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * @param <T> The Java type of the service to locate
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
	 * @param <T> The Java type of the service to locate
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
