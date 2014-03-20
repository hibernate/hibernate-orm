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
package org.hibernate.envers.event.spi;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.jboss.logging.Logger;

/**
 * Provides integration for Envers into Hibernate, which mainly means registering the proper event listeners.
 *
 * @author Steve Ebersole
 */
public class EnversIntegrator implements Integrator {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			EnversIntegrator.class.getName()
	);

	/**
	 * The name of a configuration setting that can be used to control whether auto registration of envers listeners
	 * should happen or not.  Default is true
	 */
	public static final String AUTO_REGISTER = "hibernate.listeners.envers.autoRegister";
    private AuditConfiguration enversConfiguration;

	@Override
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		final boolean autoRegister = ConfigurationHelper.getBoolean(
				AUTO_REGISTER,
				configuration.getProperties(),
				true
		);
		if ( !autoRegister ) {
			LOG.debug( "Skipping Envers listener auto registration" );
			return;
		}

		final EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		listenerRegistry.addDuplicationStrategy( EnversListenerDuplicationStrategy.INSTANCE );

		if ( enversConfiguration.getEntCfg().hasAuditedEntities() ) {
			listenerRegistry.appendListeners(
					EventType.POST_DELETE, new EnversPostDeleteEventListenerImpl(
					enversConfiguration
			)
			);
			listenerRegistry.appendListeners(
					EventType.POST_INSERT, new EnversPostInsertEventListenerImpl(
					enversConfiguration
			)
			);
			listenerRegistry.appendListeners(
					EventType.POST_UPDATE, new EnversPostUpdateEventListenerImpl(
					enversConfiguration
			)
			);
			listenerRegistry.appendListeners(
					EventType.POST_COLLECTION_RECREATE,
					new EnversPostCollectionRecreateEventListenerImpl( enversConfiguration )
			);
			listenerRegistry.appendListeners(
					EventType.PRE_COLLECTION_REMOVE,
					new EnversPreCollectionRemoveEventListenerImpl( enversConfiguration )
			);
			listenerRegistry.appendListeners(
					EventType.PRE_COLLECTION_UPDATE,
					new EnversPreCollectionUpdateEventListenerImpl( enversConfiguration )
			);
			throw new AssertionFailure( "No longer implemented." );
		}
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		if ( enversConfiguration != null ) {
			enversConfiguration.destroy();
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.integrator.spi.Integrator#integrate(org.hibernate.metamodel.spi.MetadataImplementor, org.hibernate.engine.spi.SessionFactoryImplementor, org.hibernate.service.spi.SessionFactoryServiceRegistry)
	 */
	@Override
	public void integrate(
			MetadataImplementor metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
		final boolean autoRegister = configurationService.getSetting(
				AUTO_REGISTER,
				StandardConverters.BOOLEAN,
				true
		);
		if ( !autoRegister ) {
			LOG.debug( "Skipping Envers listener auto registration" );
			return;
		}

		final EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		listenerRegistry.addDuplicationStrategy( EnversListenerDuplicationStrategy.INSTANCE );

		enversConfiguration = AuditConfiguration.register(
				null,
				metadata
		);

		if ( enversConfiguration.getEntCfg().hasAuditedEntities() ) {
			listenerRegistry.appendListeners(
					EventType.POST_DELETE, new EnversPostDeleteEventListenerImpl(
					enversConfiguration
			)
			);
			listenerRegistry.appendListeners(
					EventType.POST_INSERT, new EnversPostInsertEventListenerImpl(
					enversConfiguration
			)
			);
			listenerRegistry.appendListeners(
					EventType.POST_UPDATE, new EnversPostUpdateEventListenerImpl(
					enversConfiguration
			)
			);
			listenerRegistry.appendListeners(
					EventType.POST_COLLECTION_RECREATE,
					new EnversPostCollectionRecreateEventListenerImpl( enversConfiguration )
			);
			listenerRegistry.appendListeners(
					EventType.PRE_COLLECTION_REMOVE,
					new EnversPreCollectionRemoveEventListenerImpl( enversConfiguration )
			);
			listenerRegistry.appendListeners(
					EventType.PRE_COLLECTION_UPDATE,
					new EnversPreCollectionUpdateEventListenerImpl( enversConfiguration )
			);
		}
	}
}
