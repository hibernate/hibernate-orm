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
package org.hibernate.envers.boot.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.event.spi.EnversListenerDuplicationStrategy;
import org.hibernate.envers.event.spi.EnversPostCollectionRecreateEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPostDeleteEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPostInsertEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPostUpdateEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPreCollectionRemoveEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPreCollectionUpdateEventListenerImpl;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Hooks up Envers event listeners.
 *
 * @author Steve Ebersole
 */
public class EnversIntegrator implements Integrator {
	public static final String AUTO_REGISTER = EnversService.LEGACY_AUTO_REGISTER;

	public void integrate(
			Metadata metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		final EnversService enversService = serviceRegistry.getService( EnversService.class );
		if ( !enversService.isEnabled() ) {
			return;
		}

		if ( !enversService.isInitialized() ) {
			throw new HibernateException(
					"Expecting EnversService to have been initialized prior to call to EnversIntegrator#integrate"
			);
		}

		final EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		listenerRegistry.addDuplicationStrategy( EnversListenerDuplicationStrategy.INSTANCE );

		if ( enversService.getEntitiesConfigurations().hasAuditedEntities() ) {
			listenerRegistry.appendListeners(
					EventType.POST_DELETE,
					new EnversPostDeleteEventListenerImpl( enversService )
			);
			listenerRegistry.appendListeners(
					EventType.POST_INSERT,
					new EnversPostInsertEventListenerImpl( enversService )
			);
			listenerRegistry.appendListeners(
					EventType.POST_UPDATE,
					new EnversPostUpdateEventListenerImpl( enversService )
			);
			listenerRegistry.appendListeners(
					EventType.POST_COLLECTION_RECREATE,
					new EnversPostCollectionRecreateEventListenerImpl( enversService )
			);
			listenerRegistry.appendListeners(
					EventType.PRE_COLLECTION_REMOVE,
					new EnversPreCollectionRemoveEventListenerImpl( enversService )
			);
			listenerRegistry.appendListeners(
					EventType.PRE_COLLECTION_UPDATE,
					new EnversPreCollectionUpdateEventListenerImpl( enversService )
			);
		}
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// nothing to do
	}
}
