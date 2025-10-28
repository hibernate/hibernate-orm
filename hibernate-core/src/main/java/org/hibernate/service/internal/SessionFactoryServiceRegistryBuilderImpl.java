/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryBuilder;

import static org.hibernate.service.internal.StandardSessionFactoryServiceInitiators.buildStandardServiceInitiatorList;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryServiceRegistryBuilderImpl implements SessionFactoryServiceRegistryBuilder {
	private final ServiceRegistryImplementor parent;

	private final List<SessionFactoryServiceInitiator<?>> initiators = buildStandardServiceInitiatorList();
	private final List<ProvidedService<? extends Service>> providedServices = new ArrayList<>();

	public SessionFactoryServiceRegistryBuilderImpl(ServiceRegistryImplementor parent) {
		this.parent = parent;
		if ( parent != null ) {
			for ( var iterator = initiators.iterator(); iterator.hasNext(); ) {
				final var initiator = iterator.next();
				if ( parent.locateServiceBinding( initiator.getServiceInitiated() ) != null ) {
					// Parent takes precedence over the standard service initiators
					iterator.remove();
				}
			}

		}
	}

	/**
	 * Adds a service initiator.
	 *
	 * @param initiator The initiator to be added
	 *
	 * @return this, for method chaining
	 */
	@Override
	public SessionFactoryServiceRegistryBuilder addInitiator(SessionFactoryServiceInitiator<?> initiator) {
		initiators.add( initiator );
		return this;
	}

	/**
	 * Adds a user-provided service.
	 *
	 * @param serviceRole The role of the service being added
	 * @param service The service implementation
	 *
	 * @return this, for method chaining
	 */
	@Override
	public <R extends Service> SessionFactoryServiceRegistryBuilder addService(final Class<R> serviceRole, final R service) {
		providedServices.add( new ProvidedService<>( serviceRole, service ) );
		return this;
	}

	public SessionFactoryServiceRegistry buildSessionFactoryServiceRegistry(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions options) {
		return SessionFactoryServiceRegistryImpl.create(
				parent,
				initiators,
				providedServices,
				sessionFactory,
				options
		);
	}
}
