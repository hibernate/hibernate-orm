/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.spi;

import org.hibernate.service.Service;

import org.jboss.logging.Logger;

/**
 * Models a binding for a particular service
 *
 * @author Steve Ebersole
 */
public final class ServiceBinding<R extends Service> {
	private static final Logger log = Logger.getLogger( ServiceBinding.class );

	public static interface ServiceLifecycleOwner {
		public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator);

		public <R extends Service> void configureService(ServiceBinding<R> binding);
		public <R extends Service> void injectDependencies(ServiceBinding<R> binding);
		public <R extends Service> void startService(ServiceBinding<R> binding);

		public <R extends Service> void stopService(ServiceBinding<R> binding);
	}

	private final ServiceLifecycleOwner lifecycleOwner;
	private final Class<R> serviceRole;
	private final ServiceInitiator<R> serviceInitiator;
	private R service;

	public ServiceBinding(ServiceLifecycleOwner lifecycleOwner, Class<R> serviceRole, R service) {
		this.lifecycleOwner = lifecycleOwner;
		this.serviceRole = serviceRole;
		this.serviceInitiator = null;
		this.service = service;
	}

	public ServiceBinding(ServiceLifecycleOwner lifecycleOwner, ServiceInitiator<R> serviceInitiator) {
		this.lifecycleOwner = lifecycleOwner;
		this.serviceRole = serviceInitiator.getServiceInitiated();
		this.serviceInitiator = serviceInitiator;
	}

	public ServiceLifecycleOwner getLifecycleOwner() {
		return lifecycleOwner;
	}

	public Class<R> getServiceRole() {
		return serviceRole;
	}

	public ServiceInitiator<R> getServiceInitiator() {
		return serviceInitiator;
	}

	public R getService() {
		return service;
	}

	public void setService(R service) {
		if ( this.service != null ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "Overriding existing service binding [" + serviceRole.getName() + "]" );
			}
		}
		this.service = service;
	}
}
