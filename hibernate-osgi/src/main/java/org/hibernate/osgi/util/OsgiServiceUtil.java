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
package org.hibernate.osgi.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.internal.CoreMessageLogger;
import org.jboss.logging.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author Brett Meyer
 */
public class OsgiServiceUtil {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class,
			OsgiServiceUtil.class.getName() );

	public static <T> List<T> getServiceImpls(Class<T> contract, BundleContext context) {
		List<T> serviceImpls = new ArrayList<T>();
		try {
			Collection<ServiceReference<T>> serviceRefs = context.getServiceReferences( contract, null );
			for ( ServiceReference<T> serviceRef : serviceRefs ) {
				serviceImpls.add( context.getService( serviceRef ) );
			}
		}
		catch ( Exception e ) {
			LOG.unableToDiscoverOsgiService( contract.getName(), e );
		}
		return serviceImpls;
	}
}
