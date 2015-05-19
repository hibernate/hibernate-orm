/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
