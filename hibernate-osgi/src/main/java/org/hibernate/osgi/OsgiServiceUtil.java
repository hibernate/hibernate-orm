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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.logging.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Utilities for dealing with OSGi environments
 *
 * @author Brett Meyer
 */
public class OsgiServiceUtil {
	private static final Logger LOG = Logger.getLogger( OsgiServiceUtil.class );

	/**
	 * Locate all implementors of the given service contract in the given OSGi buindle context
	 *
	 * @param contract The service contract for which to locate implementors
	 * @param context The OSGi bundle context
	 * @param <T> The Java type of the service to locate
	 *
	 * @return All know implementors
	 */
	public static <T> List<T> getServiceImpls(Class<T> contract, BundleContext context) {
		final List<T> serviceImpls = new ArrayList<T>();
		try {
			final Collection<ServiceReference<T>> serviceRefs = context.getServiceReferences( contract, null );
			for ( ServiceReference<T> serviceRef : serviceRefs ) {
				serviceImpls.add( context.getService( serviceRef ) );
			}
		}
		catch ( Exception e ) {
			LOG.warnf( e, "Exception while discovering OSGi service implementations : %s", contract.getName() );
		}
		return serviceImpls;
	}

	private OsgiServiceUtil() {
	}
}
