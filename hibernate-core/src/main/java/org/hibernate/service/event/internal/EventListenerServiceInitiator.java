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
package org.hibernate.service.event.internal;

import org.hibernate.service.event.spi.EventListenerRegistry;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistry;

import java.util.Map;

/**
 * Service initiator for {@link EventListenerRegistry}
 *
 * @author Steve Ebersole
 */
public class EventListenerServiceInitiator implements ServiceInitiator<EventListenerRegistry> {
	public static final EventListenerServiceInitiator INSTANCE = new EventListenerServiceInitiator();

	@Override
	public Class<EventListenerRegistry> getServiceInitiated() {
		return EventListenerRegistry.class;
	}

	@Override
	public EventListenerRegistry initiateService(Map configurationValues, ServiceRegistry registry) {
		return new EventListenerRegistryImpl();
	}
}