/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.spi;

import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.ServiceContributingIntegrator;
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
			Metadata metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		doIntegration(
				serviceRegistry.getService( ConfigurationService.class ).getSettings(),
				// pass no permissions here, because atm actually injecting the
				// permissions into the JaccService is handled on SessionFactoryImpl via
				// the org.hibernate.boot.cfgxml.spi.CfgXmlAccessService
				null,
				serviceRegistry
		);
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
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// nothing to do
	}
}
