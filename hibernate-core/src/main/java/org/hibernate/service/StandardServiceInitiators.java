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
package org.hibernate.service;

import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.transaction.internal.TransactionFactoryInitiator;
import org.hibernate.event.EventListenerRegistration;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.internal.PersisterFactoryInitiator;
import org.hibernate.service.classloading.internal.ClassLoaderServiceInitiator;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.service.event.internal.EventListenerServiceInitiator;
import org.hibernate.service.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.service.jdbc.connections.internal.MultiTenantConnectionProviderInitiator;
import org.hibernate.service.jdbc.dialect.internal.DialectFactoryInitiator;
import org.hibernate.service.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.service.jmx.internal.JmxServiceInitiator;
import org.hibernate.service.jndi.internal.JndiServiceInitiator;
import org.hibernate.service.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class StandardServiceInitiators {
	public static List<BasicServiceInitiator> LIST = buildStandardServiceInitiatorList();

	private static List<BasicServiceInitiator> buildStandardServiceInitiatorList() {
		final List<BasicServiceInitiator> serviceInitiators = new ArrayList<BasicServiceInitiator>();

		serviceInitiators.add( ClassLoaderServiceInitiator.INSTANCE );
		serviceInitiators.add( JndiServiceInitiator.INSTANCE );
		serviceInitiators.add( JmxServiceInitiator.INSTANCE );

		serviceInitiators.add( PersisterClassResolverInitiator.INSTANCE );
		serviceInitiators.add( PersisterFactoryInitiator.INSTANCE );

		serviceInitiators.add( ConnectionProviderInitiator.INSTANCE );
		serviceInitiators.add( MultiTenantConnectionProviderInitiator.INSTANCE );
		serviceInitiators.add( DialectResolverInitiator.INSTANCE );
		serviceInitiators.add( DialectFactoryInitiator.INSTANCE );
		serviceInitiators.add( BatchBuilderInitiator.INSTANCE );
		serviceInitiators.add( JdbcServicesInitiator.INSTANCE );

		serviceInitiators.add( JtaPlatformInitiator.INSTANCE );
		serviceInitiators.add( TransactionFactoryInitiator.INSTANCE );

		serviceInitiators.add( SessionFactoryServiceRegistryFactoryInitiator.INSTANCE );

		serviceInitiators.add( EventListenerRegistrationServiceInitiator.INSTANCE );

		return serviceInitiators;
	}


	// todo : completely temporary.  See HHH-5562 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Acts as a service in the basic registry to which users/integrators can attach things that perform event listener
	 * registration.  The event listeners live in the SessionFactory registry, but it has access to the basic registry.
	 * So when it starts up, it looks in the basic registry for this service and does the requested  registrations.
	 */
	public static interface EventListenerRegistrationService extends Service {
		public void attachEventListenerRegistration(EventListenerRegistration registration);
		public Iterable<EventListenerRegistration> getEventListenerRegistrations();
	}

	public static class EventListenerRegistrationServiceImpl implements EventListenerRegistrationService {
		private List<EventListenerRegistration> registrations = new ArrayList<EventListenerRegistration>();

		@Override
		public void attachEventListenerRegistration(EventListenerRegistration registration) {
			registrations.add( registration );
		}

		@Override
		public Iterable<EventListenerRegistration> getEventListenerRegistrations() {
			return registrations;
		}
	}

	public static class EventListenerRegistrationServiceInitiator implements BasicServiceInitiator<EventListenerRegistrationService> {
		public static final EventListenerRegistrationServiceInitiator INSTANCE = new EventListenerRegistrationServiceInitiator();

		@Override
		public Class<EventListenerRegistrationService> getServiceInitiated() {
			return EventListenerRegistrationService.class;
		}

		@Override
		public EventListenerRegistrationService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
			return new EventListenerRegistrationServiceImpl();
		}
	}
}
