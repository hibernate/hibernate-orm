/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.secure.spi;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.ServiceContributingIntegrator;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.secure.internal.DisabledJaccServiceImpl;
import org.hibernate.secure.internal.JaccPreDeleteEventListener;
import org.hibernate.secure.internal.JaccPreInsertEventListener;
import org.hibernate.secure.internal.JaccPreLoadEventListener;
import org.hibernate.secure.internal.JaccPreUpdateEventListener;
import org.hibernate.secure.internal.JaccSecurityListener;
import org.hibernate.secure.internal.StandardJaccServiceImpl;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.jboss.logging.Logger;

/**
 * Integrator for setting up JACC integration
 *
 * @author Steve Ebersole
 */
public class JaccIntegrator implements ServiceContributingIntegrator {
	private static final Logger log = Logger.getLogger( JaccIntegrator.class );

	private static final DuplicationStrategy DUPLICATION_STRATEGY = new DuplicationStrategy() {
		@Override
		public boolean areMatch(Object listener, Object original) {
			return listener.getClass().equals( original.getClass() ) &&
					JaccSecurityListener.class.isInstance( original );
		}

		@Override
		public Action getAction() {
			return Action.KEEP_ORIGINAL;
		}
	};

	@Override
	public void prepareServices(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		boolean isSecurityEnabled = serviceRegistryBuilder.getSettings().containsKey( AvailableSettings.JACC_ENABLED );
		final JaccService jaccService = isSecurityEnabled ? new StandardJaccServiceImpl() : new DisabledJaccServiceImpl();
		serviceRegistryBuilder.addService( JaccService.class, jaccService );
	}

	@Override
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		doIntegration( configuration.getProperties(), null, serviceRegistry );
	}

	private void doIntegration(
			Map properties,
			JaccPermissionDeclarations permissionDeclarations,
			SessionFactoryServiceRegistry serviceRegistry) {
		boolean isSecurityEnabled = properties.containsKey( AvailableSettings.JACC_ENABLED );
		if ( ! isSecurityEnabled ) {
			log.debug( "Skipping JACC integration as it was not enabled" );
			return;
		}

		final String contextId = (String) properties.get( AvailableSettings.JACC_CONTEXT_ID );
		if ( contextId == null ) {
			throw new IntegrationException( "JACC context id must be specified" );
		}

		final JaccService jaccService = serviceRegistry.getService( JaccService.class );
		if ( jaccService == null ) {
			throw new IntegrationException( "JaccService was not set up" );
		}

		if ( permissionDeclarations != null ) {
			for ( GrantedPermission declaration : permissionDeclarations.getPermissionDeclarations() ) {
				jaccService.addPermission( declaration );
			}
		}

		final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		eventListenerRegistry.addDuplicationStrategy( DUPLICATION_STRATEGY );

		eventListenerRegistry.prependListeners( EventType.PRE_DELETE, new JaccPreDeleteEventListener() );
		eventListenerRegistry.prependListeners( EventType.PRE_INSERT, new JaccPreInsertEventListener() );
		eventListenerRegistry.prependListeners( EventType.PRE_UPDATE, new JaccPreUpdateEventListener() );
		eventListenerRegistry.prependListeners( EventType.PRE_LOAD, new JaccPreLoadEventListener() );
	}

	@Override
	public void integrate(
			MetadataImplementor metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		// todo : need to stash the JACC permissions somewhere accessible from here...
		doIntegration( sessionFactory.getProperties(), null, serviceRegistry );
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// nothing to do
	}
}
