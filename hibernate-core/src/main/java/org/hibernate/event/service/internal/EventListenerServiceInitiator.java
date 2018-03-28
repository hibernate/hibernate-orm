/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.internal;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;

/**
 * Service initiator for {@link EventListenerRegistry}
 *
 * @author Steve Ebersole
 */
public class EventListenerServiceInitiator implements SessionFactoryServiceInitiator<EventListenerRegistry> {
	public static final EventListenerServiceInitiator INSTANCE = new EventListenerServiceInitiator();

	@Override
	public Class<EventListenerRegistry> getServiceInitiated() {
		return EventListenerRegistry.class;
	}

	@Override
	public EventListenerRegistry initiateService(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions,
			ServiceRegistryImplementor registry) {
		return new EventListenerRegistryImpl( sessionFactory, sessionFactoryOptions, registry );
	}

	@Override
	public EventListenerRegistry initiateService(SessionFactoryServiceInitiatorContext context) {
		return new EventListenerRegistryImpl( context.getSessionFactory(), context.getSessionFactoryOptions(), context.getServiceRegistry());
	}
}
