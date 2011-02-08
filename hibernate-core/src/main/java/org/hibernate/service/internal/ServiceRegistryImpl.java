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
package org.hibernate.service.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.Service;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistry;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.service.spi.UnknownServiceException;

/**
 * Basic Hibernate implementation of the service registry.
 *
 * @author Steve Ebersole
 */
public class ServiceRegistryImpl implements ServiceRegistry {
	private static final Logger log = LoggerFactory.getLogger( ServiceRegistryImpl.class );

	private final List<ServiceInitiator> serviceInitiators;
	private ServicesInitializer initializer;

	private HashMap<Class,Service> serviceMap = new HashMap<Class, Service>();
	// IMPL NOTE : the list used for ordered destruction.  Cannot used ordered map above because we need to
	// iterate it in reverse order which is only available through ListIterator
	private List<Service> serviceList = new ArrayList<Service>();

	public ServiceRegistryImpl(List<ServiceInitiator> serviceInitiators) {
		this.serviceInitiators = Collections.unmodifiableList( serviceInitiators );
	}

	public void initialize(Map configurationValues) {
		this.initializer = new ServicesInitializer( this, serviceInitiators, ConfigurationHelper.clone( configurationValues ) );
	}

	public void destroy() {
		ListIterator<Service> serviceIterator = serviceList.listIterator();
		while ( serviceIterator.hasPrevious() ) {
			final Service service = serviceIterator.previous();
			if ( Stoppable.class.isInstance( service ) ) {
				try {
					( (Stoppable) service ).stop();
				}
				catch ( Exception e ) {
					log.info( "Error stopping service [" + service.getClass() + "] : " + e.toString() );
				}
			}
		}
		serviceList.clear();
		serviceList = null;
		serviceMap.clear();
		serviceMap = null;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <T extends Service> T getService(Class<T> serviceRole) {
		T service = internalGetService( serviceRole );
		if ( service == null ) {
			throw new UnknownServiceException( serviceRole );
		}
		return service;
	}

	@SuppressWarnings({ "unchecked" })
	private <T extends Service> T locateService(Class<T> serviceRole) {
		return (T) serviceMap.get( serviceRole );
	}

	<T extends Service> T internalGetService(Class<T> serviceRole) {
		T service = locateService( serviceRole );
		if ( service == null ) {
			service = initializer.initializeService( serviceRole );
		}
		return service;
	}

	<T extends Service> void registerService(Class<T> serviceRole, T service) {
		serviceList.add( service );
		serviceMap.put( serviceRole, service );
	}
}
