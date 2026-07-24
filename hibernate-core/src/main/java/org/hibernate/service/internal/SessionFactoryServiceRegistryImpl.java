/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

import java.util.List;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.metamodel.spi.SessionFactoryAccess;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import jakarta.annotation.Nullable;

import static org.hibernate.service.internal.ServiceLogger.SERVICE_LOGGER;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryServiceRegistryImpl
		extends AbstractServiceRegistryImpl
		implements SessionFactoryServiceRegistry, SessionFactoryServiceInitiatorContext {

	private final SessionFactoryOptions sessionFactoryOptions;
	private final SessionFactoryAccess sessionFactoryAccess;

	private SessionFactoryServiceRegistryImpl(
			ServiceRegistryImplementor parent,
			SessionFactoryAccess sessionFactoryAccess,
			SessionFactoryOptions sessionFactoryOptions) {
		super( parent );
		this.sessionFactoryAccess = sessionFactoryAccess;
		this.sessionFactoryOptions = sessionFactoryOptions;
	}

	public static SessionFactoryServiceRegistryImpl create(
			ServiceRegistryImplementor parent,
			List<SessionFactoryServiceInitiator<?>> initiators,
			List<ProvidedService<? extends Service>> providedServices,
			SessionFactoryAccess sessionFactoryAccess,
			SessionFactoryOptions sessionFactoryOptions) {
		final var instance = new SessionFactoryServiceRegistryImpl( parent, sessionFactoryAccess, sessionFactoryOptions );
		instance.initialize( initiators, providedServices );
		return instance;
	}

	protected void initialize(
			List<SessionFactoryServiceInitiator<?>> initiators,
			List<ProvidedService<? extends Service>> providedServices) {
		super.initialize();
		// for now, just use the standard initiator list
		for ( var initiator : initiators ) {
			// create the bindings up front to help identify to which registry services belong
			createServiceBinding( initiator );
		}

		for ( var providedService : providedServices ) {
			createServiceBinding( providedService );
		}
	}

	@Override
	public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		final var sessionFactoryServiceInitiator = (SessionFactoryServiceInitiator<R>) serviceInitiator;
		return sessionFactoryServiceInitiator.initiateService( this );
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
		if ( serviceBinding.getService() instanceof Configurable configurable ) {
			configurable.configure( requireService( ConfigurationService.class ).getSettings() );
		}
	}

	@Override
	@Nonnull
	public SessionFactoryAccess getSessionFactoryAccess() {
		return sessionFactoryAccess;
	}

	@Override
	@Nonnull
	public SessionFactoryOptions getSessionFactoryOptions() {
		return sessionFactoryOptions;
	}

	@Override
	@Nonnull
	public ServiceRegistryImplementor getServiceRegistry() {
		return this;
	}

	@Override
	public <R extends Service> @Nullable R getService(@Nonnull Class<R> serviceRole) {
		if ( serviceRole.equals( EventListenerRegistry.class ) ) {
			SERVICE_LOGGER.eventListenerRegistryAccessDeprecated();
			//noinspection unchecked
			return (R) sessionFactoryAccess.getSessionFactory().getEventEngine().getListenerRegistry();
		}
		else {
			return super.getService( serviceRole );
		}
	}
}
