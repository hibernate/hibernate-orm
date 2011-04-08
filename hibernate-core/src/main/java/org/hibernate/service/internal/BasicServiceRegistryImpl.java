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

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.service.Service;
import org.hibernate.service.StandardServiceInitiators;
import org.hibernate.service.UnknownServiceException;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryAwareService;

/**
 * Standard Hibernate implementation of the service registry.
 *
 * @author Steve Ebersole
 */
public class BasicServiceRegistryImpl extends AbstractServiceRegistryImpl implements BasicServiceRegistry {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, BasicServiceRegistryImpl.class.getName());

	private final Map<Class,BasicServiceInitiator> serviceInitiatorMap;
	private final Map configurationValues;

	public BasicServiceRegistryImpl(Map configurationValues) {
		this( StandardServiceInitiators.LIST, configurationValues );
	}

	@SuppressWarnings( {"unchecked"})
	public BasicServiceRegistryImpl(List<BasicServiceInitiator> serviceInitiators, Map configurationValues) {
		super();
		this.serviceInitiatorMap = toMap( serviceInitiators );
		this.configurationValues = configurationValues;
		for ( BasicServiceInitiator initiator : serviceInitiatorMap.values() ) {
			// create the bindings up front to help identify to which registry services belong
			createServiceBinding( initiator.getServiceInitiated() );
		}
	}

	/**
	 * We convert the incoming list of initiators to a map for 2 reasons:<ul>
	 * <li>to make it easier to look up the initiator we need for a given service role</li>
	 * <li>to make sure there is only one initiator for a given service role (last wins)</li>
	 * </ul>
	 *
	 * @param serviceInitiators The list of individual initiators
	 *
	 * @return The map of initiators keyed by the service rle they initiate.
	 */
	private static Map<Class, BasicServiceInitiator> toMap(List<BasicServiceInitiator> serviceInitiators) {
		final Map<Class, BasicServiceInitiator> result = new HashMap<Class, BasicServiceInitiator>();
		for ( BasicServiceInitiator initiator : serviceInitiators ) {
			result.put( initiator.getServiceInitiated(), initiator );
		}
		return result;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public void registerServiceInitiator(BasicServiceInitiator initiator) {
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
		BasicServiceInitiator<T> initiator = serviceInitiatorMap.get( serviceRole );
		if ( initiator == null ) {
			throw new UnknownServiceException( serviceRole );
		}
		try {
			T service = initiator.initiateService( configurationValues, this );
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

		if ( Configurable.class.isInstance( service ) ) {
			( (Configurable) service ).configure( configurationValues );
		}
	}
}
