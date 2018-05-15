/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.AuditService;
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
 * Hooks up Envers event listeners and initializes the {@link AuditService}.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public class EnversIntegrator implements Integrator {
	private static final Logger LOG = Logger.getLogger( EnversIntegrator.class );

	public static final String AUTO_REGISTER = "hibernate.envers.autoRegisterListeners";

	private AuditService auditService;
	private EnversService enversService;

	public void integrate(
			Metadata metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {

		this.enversService = serviceRegistry.getService( EnversService.class );
		this.auditService = serviceRegistry.getService( AuditService.class );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Opt-out of registration if EnversService is disabled
		if ( !enversService.isEnabled() ) {
			LOG.debug( "Skipping Envers listener registrations : EnversService disabled" );
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
			LOG.debug( "Skipping Envers listener registrations : Listener auto-registration disabled" );
			return;
		}

		initializeMetadata( (MetadataImplementor) metadata );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Opt-out of registration if no audited entities found
		if ( !auditService.getEntityBindings().hasAuditedEntities() ) {
			LOG.debug( "Skipping Envers listener registrations : No audited entities found" );
			return;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Wire up the listeners.
		// At this point, it's known that at least one audited entity is bound.
		final EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		listenerRegistry.addDuplicationStrategy( EnversListenerDuplicationStrategy.INSTANCE );
		registerListeners( listenerRegistry, auditService );
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {

	}

	private void initializeMetadata(MetadataImplementor metadata) {
		auditService.initialize( metadata.getAuditMetadataBuilder().build() );
	}

	private void registerListeners(final EventListenerRegistry listenerRegistry, final AuditService auditService) {
		listenerRegistry.appendListeners(
				EventType.POST_DELETE,
				new EnversPostDeleteEventListenerImpl( auditService )
		);
		listenerRegistry.appendListeners(
				EventType.POST_INSERT,
				new EnversPostInsertEventListenerImpl( auditService )
		);
		listenerRegistry.appendListeners(
				EventType.PRE_UPDATE,
				new EnversPreUpdateEventListenerImpl( auditService )
		);
		listenerRegistry.appendListeners(
				EventType.POST_UPDATE,
				new EnversPostUpdateEventListenerImpl( auditService )
		);
		listenerRegistry.appendListeners(
				EventType.POST_COLLECTION_RECREATE,
				new EnversPostCollectionRecreateEventListenerImpl( auditService )
		);
		listenerRegistry.appendListeners(
				EventType.PRE_COLLECTION_REMOVE,
				new EnversPreCollectionRemoveEventListenerImpl( auditService )
		);
		listenerRegistry.appendListeners(
				EventType.PRE_COLLECTION_UPDATE,
				new EnversPreCollectionUpdateEventListenerImpl( auditService )
		);
	}
}
