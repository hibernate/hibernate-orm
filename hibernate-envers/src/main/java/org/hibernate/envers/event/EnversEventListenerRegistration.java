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
package org.hibernate.envers.event;

import java.util.Map;

import org.hibernate.cfg.Configuration;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.event.EventListenerRegistration;
import org.hibernate.event.EventType;
import org.hibernate.service.event.spi.EventListenerRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * See "transitory" notes on {@link EventListenerRegistration}
 * 
 * @author Steve Ebersole
 */
public class EnversEventListenerRegistration implements EventListenerRegistration {
	@Override
	public void apply(
			EventListenerRegistry eventListenerRegistry,
			Configuration configuration, Map<?, ?> configValues, ServiceRegistryImplementor serviceRegistry
	) {
		EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		listenerRegistry.addDuplicationStrategy( EnversListenerDuplicationStrategy.INSTANCE );

		// todo : build AuditConfiguration; requires massive changes...
//		AuditConfiguration auditConfiguration = AuditConfiguration.getFor( ??? )
		AuditConfiguration auditConfiguration = null;

		// create/add listeners
		listenerRegistry.appendListeners( EventType.POST_DELETE, new EnversPostDeleteEventListenerImpl( auditConfiguration ) );
		listenerRegistry.appendListeners( EventType.POST_INSERT, new EnversPostInsertEventListenerImpl( auditConfiguration ) );
		listenerRegistry.appendListeners( EventType.POST_UPDATE, new EnversPostUpdateEventListenerImpl( auditConfiguration ) );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_RECREATE, new EnversPostCollectionRecreateEventListenerImpl( auditConfiguration ) );
		listenerRegistry.appendListeners( EventType.PRE_COLLECTION_REMOVE, new EnversPreCollectionRemoveEventListenerImpl( auditConfiguration ) );
		listenerRegistry.appendListeners( EventType.PRE_COLLECTION_UPDATE, new EnversPreCollectionUpdateEventListenerImpl( auditConfiguration ) );
	}
}
