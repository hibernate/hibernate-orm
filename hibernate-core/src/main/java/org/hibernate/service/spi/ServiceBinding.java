/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import org.hibernate.service.Service;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Models a binding for a particular service.
 *
 * @author Steve Ebersole
 */
public final class ServiceBinding<R extends Service> {
	private static final Logger LOG = Logger.getLogger( ServiceBinding.class );

	public interface ServiceLifecycleOwner {
		<R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator);

		<R extends Service> void configureService(ServiceBinding<R> binding);
		<R extends Service> void injectDependencies(ServiceBinding<R> binding);
		<R extends Service> void startService(ServiceBinding<R> binding);

		<R extends Service> void stopService(ServiceBinding<R> binding);
	}

	private final ServiceLifecycleOwner lifecycleOwner;
	private final Class<R> serviceRole;
	private final @Nullable ServiceInitiator<R> serviceInitiator;
	private volatile R service;

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

	public @Nullable ServiceInitiator<R> getServiceInitiator() {
		return serviceInitiator;
	}

	public R getService() {
		return service;
	}

	public void setService(R service) {
		if ( this.service != null ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debug( "Overriding existing service binding [" + serviceRole.getName() + "]" );
			}
		}
		this.service = service;
	}
}
