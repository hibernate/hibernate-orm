/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.event.spi.EnversListenerDuplicationStrategy;
import org.hibernate.envers.event.spi.EnversPostCollectionRecreateEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPostDeleteEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPostInsertEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPostUpdateEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPreCollectionRemoveEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPreCollectionUpdateEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPreUpdateEventListenerImpl;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.jboss.logging.Logger;

/**
 * Hooks up Envers event listeners.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public class EnversIntegrator implements Integrator {
	private static final Logger log = Logger.getLogger( EnversIntegrator.class );

	public static final String AUTO_REGISTER = "hibernate.envers.autoRegisterListeners";

	public void integrate(
			Metadata metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		final EnversService enversService = serviceRegistry.getService( EnversService.class );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Opt-out of registration if EnversService is disabled
		if ( !enversService.isEnabled() ) {
			log.debug( "Skipping Envers listener registrations : EnversService disabled" );
			return;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Opt-out of registration if asked to not register
		final boolean autoRegister = serviceRegistry.getService( ConfigurationService.class ).getSetting(
				AUTO_REGISTER,
				StandardConverters.BOOLEAN,
				true
		);
		if ( !autoRegister ) {
			log.debug( "Skipping Envers listener registrations : Listener auto-registration disabled" );
			return;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Verify that the EnversService is fully initialized and ready to go.
		if ( !enversService.isInitialized() ) {
			throw new HibernateException(
					"Expecting EnversService to have been initialized prior to call to EnversIntegrator#integrate"
			);
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Opt-out of registration if no audited entities found
		if ( !enversService.getEntitiesConfigurations().hasAuditedEntities() ) {
			log.debug( "Skipping Envers listener registrations : No audited entities found" );
			return;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Do the registrations
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
					EventType.PRE_UPDATE,
					new EnversPreUpdateEventListenerImpl( enversService )
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
