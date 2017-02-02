/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi;

import java.util.LinkedHashSet;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

/**
 * The ClassLoaderService that should be used when running in OSGi;
 * this helps for example to load the Services correctly.
 *
 * @author Sanne Grinovero
 */
public class OSGiClassLoaderServiceImpl extends ClassLoaderServiceImpl implements ClassLoaderService {

	private final OsgiServiceUtil osgiServiceUtil;
	private final OsgiClassLoader osgiClassLoader;

	public OSGiClassLoaderServiceImpl(OsgiClassLoader osgiClassLoader, OsgiServiceUtil osgiServiceUtil) {
		super( osgiClassLoader );
		this.osgiClassLoader = osgiClassLoader;
		this.osgiServiceUtil = osgiServiceUtil;
	}

	@Override
	public <S> LinkedHashSet<S> loadJavaServices(Class<S> serviceContract) {
		Iterable<S> parentDiscoveredServices = super.loadJavaServices( serviceContract );
		S[] serviceImpls = osgiServiceUtil.getServiceImpls(serviceContract);
		LinkedHashSet<S> composite = new LinkedHashSet<S>();
		for ( S service : parentDiscoveredServices ) {
			composite.add( service );
		}
		for ( S service : serviceImpls ) {
			composite.add( service );
		}
		return composite;
	}

	@Override
	public void stop() {
		super.stop();
		osgiClassLoader.stop();
		osgiServiceUtil.stop();
	}

}
