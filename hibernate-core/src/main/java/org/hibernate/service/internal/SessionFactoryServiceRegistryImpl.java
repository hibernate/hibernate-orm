/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

import java.util.List;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryServiceRegistryImpl
		extends AbstractServiceRegistryImpl
		implements SessionFactoryServiceRegistry, SessionFactoryServiceInitiatorContext {

	private static final Logger log = Logger.getLogger( SessionFactoryServiceRegistryImpl.class );

	private final SessionFactoryOptions sessionFactoryOptions;
	private final SessionFactoryImplementor sessionFactory;

	private SessionFactoryServiceRegistryImpl(
			ServiceRegistryImplementor parent,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions) {
		super( parent );
		this.sessionFactory = sessionFactory;
		this.sessionFactoryOptions = sessionFactoryOptions;
	}

	public static SessionFactoryServiceRegistryImpl create(
			ServiceRegistryImplementor parent,
			List<SessionFactoryServiceInitiator<?>> initiators,
			List<ProvidedService<?>> providedServices,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions) {
		final SessionFactoryServiceRegistryImpl instance =
				new SessionFactoryServiceRegistryImpl( parent, sessionFactory, sessionFactoryOptions );
		instance.initialize( initiators, providedServices );
		return instance;
	}

	protected void initialize(List<SessionFactoryServiceInitiator<?>> initiators, List<ProvidedService<?>> providedServices) {
		super.initialize();
		// for now, just use the standard initiator list
		for ( SessionFactoryServiceInitiator<?> initiator : initiators ) {
			// create the bindings up front to help identify to which registry services belong
			createServiceBinding( initiator );
		}

		for ( ProvidedService providedService : providedServices ) {
			createServiceBinding( providedService );
		}
	}

	@Override
	public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		SessionFactoryServiceInitiator<R> sessionFactoryServiceInitiator = (SessionFactoryServiceInitiator<R>) serviceInitiator;
		return sessionFactoryServiceInitiator.initiateService( this );
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
		if ( serviceBinding.getService() instanceof Configurable configurable ) {
			configurable.configure( requireService( ConfigurationService.class ).getSettings() );
		}
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public SessionFactoryOptions getSessionFactoryOptions() {
		return sessionFactoryOptions;
	}

	@Override
	public ServiceRegistryImplementor getServiceRegistry() {
		return this;
	}

	@Override
	public <R extends Service> @Nullable R getService(Class<R> serviceRole) {
		if ( serviceRole.equals( EventListenerRegistry.class ) ) {
			log.debug(
					"EventListenerRegistry access via ServiceRegistry is deprecated - "
						+ "use 'sessionFactory.getEventEngine().getListenerRegistry()' instead"
			);

			//noinspection unchecked
			return (R) sessionFactory.getEventEngine().getListenerRegistry();
		}

		return super.getService( serviceRole );
	}
}
