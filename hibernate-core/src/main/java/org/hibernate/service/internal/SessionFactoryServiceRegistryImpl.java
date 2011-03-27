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
package org.hibernate.service.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.HibernateLogger;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.service.Service;
import org.hibernate.service.StandardSessionFactoryServiceInitiators;
import org.hibernate.service.UnknownServiceException;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryServiceRegistryImpl
		extends AbstractServiceRegistryImpl
		implements SessionFactoryServiceRegistry {

	private static final HibernateLogger LOG = Logger.getMessageLogger( HibernateLogger.class, SessionFactoryServiceRegistryImpl.class.getName() );

	private final Map<Class,SessionFactoryServiceInitiator> serviceInitiatorMap;

	// for now we need to hold on to the Configuration... :(
	private Configuration configuration;
	private final SessionFactoryImplementor sessionFactory;

	@SuppressWarnings( {"unchecked"})
	public SessionFactoryServiceRegistryImpl(
			ServiceRegistryImplementor parent,
			SessionFactoryImplementor sessionFactory,
			Configuration configuration) {
		super( parent );
		// for now, just use the standard initiator list
		this.serviceInitiatorMap = toMap( StandardSessionFactoryServiceInitiators.LIST );

		this.sessionFactory = sessionFactory;
		this.configuration = configuration;

		for ( SessionFactoryServiceInitiator initiator : serviceInitiatorMap.values() ) {
			// create the bindings up front to help identify to which registry services belong
			createServiceBinding( initiator.getServiceInitiated() );
		}
	}

	private static Map<Class, SessionFactoryServiceInitiator> toMap(List<SessionFactoryServiceInitiator> serviceInitiators) {
		final Map<Class, SessionFactoryServiceInitiator> result = new HashMap<Class, SessionFactoryServiceInitiator>();
		for ( SessionFactoryServiceInitiator initiator : serviceInitiators ) {
			result.put( initiator.getServiceInitiated(), initiator );
		}
		return result;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public void registerServiceInitiator(SessionFactoryServiceInitiator initiator) {
		ServiceBinding serviceBinding = locateServiceBinding( initiator.getServiceInitiated(), false );
		if ( serviceBinding != null ) {
			serviceBinding.setTarget( null );
		}
		else {
			createServiceBinding( initiator.getServiceInitiated() );
		}
		final Object previous = serviceInitiatorMap.put( initiator.getServiceInitiated(), initiator );
		if ( previous != null ) {
			LOG.debugf( "Over-wrote existing service initiator [role=%s]", initiator.getServiceInitiated().getName() );
		}
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected <T extends Service> T createService(Class<T> serviceRole) {
		SessionFactoryServiceInitiator<T> initiator = serviceInitiatorMap.get( serviceRole );
		if ( initiator == null ) {
			throw new UnknownServiceException( serviceRole );
		}
		try {
			T service = initiator.initiateService( sessionFactory, configuration, this );
			// IMPL NOTE : the register call here is important to avoid potential stack overflow issues
			//		from recursive calls through #configureService
			registerService( serviceRole, service );
			return service;
		}
		catch ( ServiceException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new ServiceException( "Unable to create requested service [" + serviceRole.getName() + "]", e );
		}
	}

	@Override
	protected <T extends Service> void configureService(T service) {
		applyInjections( service );

		if ( ServiceRegistryAwareService.class.isInstance( service ) ) {
			( (ServiceRegistryAwareService) service ).injectServices( this );
		}
	}
}
