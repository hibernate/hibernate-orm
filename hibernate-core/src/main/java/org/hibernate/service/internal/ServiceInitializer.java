/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

import org.hibernate.service.jmx.spi.JmxService;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.InjectService;
import org.hibernate.service.spi.Manageable;
import org.hibernate.service.spi.Service;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.UnknownServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delegate responsible for initializing services
 *
 * @author Steve Ebersole
 */
public class ServiceInitializer {
	private static final Logger log = LoggerFactory.getLogger( ServiceInitializer.class );

	private final ServiceRegistryImpl servicesRegistry;
	private final Map<Class,ServiceInitiator> serviceInitiatorMap;
	private final Map configurationValues;

	public ServiceInitializer(
			ServiceRegistryImpl servicesRegistry,
			List<ServiceInitiator> serviceInitiators,
			Map configurationValues) {
		this.servicesRegistry = servicesRegistry;
		this.serviceInitiatorMap = toMap( serviceInitiators );
		this.configurationValues = configurationValues;
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
	private static Map<Class, ServiceInitiator> toMap(List<ServiceInitiator> serviceInitiators) {
		final Map<Class, ServiceInitiator> result = new HashMap<Class, ServiceInitiator>();
		for ( ServiceInitiator initiator : serviceInitiators ) {
			result.put( initiator.getServiceInitiated(), initiator );
		}
		return result;
	}

	void registerServiceInitiator(ServiceInitiator serviceInitiator) {
		final Object previous = serviceInitiatorMap.put( serviceInitiator.getServiceInitiated(), serviceInitiator );
		final boolean overwritten = previous != null;
		if ( overwritten ) {
			log.debug( "Over-wrote existing service initiator [role={}]", serviceInitiator.getServiceInitiated().getName() );
		}
	}

	/**
	 * The main function of this delegate.  Used to initialize the service of a given role.
	 *
	 * @param serviceRole The service role
	 * @param <T> The type of service role
	 *
	 * @return The intiialized instance of the service
	 */
	public <T extends Service> T initializeService(Class<T> serviceRole) {
		log.trace( "Initializing service [role=" + serviceRole.getName() + "]" );

		// PHASE 1 : create service
		T service = createService( serviceRole );

		// PHASE 2 : configure service (***potentially recursive***)
		configureService( service );

		// PHASE 3 : Start service
		startService( service, serviceRole );

		return service;
	}

	@SuppressWarnings({ "unchecked" })
	private <T extends Service> T createService(Class<T> serviceRole) {
		ServiceInitiator<T> initiator = (ServiceInitiator<T>) serviceInitiatorMap.get( serviceRole );
		if ( initiator == null ) {
			throw new UnknownServiceException( serviceRole );
		}
		try {
			T service = initiator.initiateService( configurationValues, servicesRegistry );
			// IMPL NOTE : the register call here is important to avoid potential stack overflow issues
			//		from recursive calls through #configureService
			servicesRegistry.registerService( serviceRole, service );
			return service;
		}
		catch ( ServiceException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new ServiceException( "Unable to create requested service [" + serviceRole.getName() + "]", e );
		}
	}

	private <T extends Service> void configureService(T service) {
		applyInjections( service );

		if ( Configurable.class.isInstance( service ) ) {
			( (Configurable) service ).configure( configurationValues );
		}

		if ( ServiceRegistryAwareService.class.isInstance( service ) ) {
			( (ServiceRegistryAwareService) service ).injectServices( servicesRegistry );
		}
	}

	private <T extends Service> void applyInjections(T service) {
		for ( Method method : service.getClass().getMethods() ) {
			InjectService injectService = method.getAnnotation( InjectService.class );
			if ( injectService == null ) {
				continue;
			}

			applyInjection( service, method, injectService );
		}
	}

	@SuppressWarnings({ "unchecked" })
	private <T extends Service> void applyInjection(T service, Method injectionMethod, InjectService injectService) {
		if ( injectionMethod.getParameterTypes() == null || injectionMethod.getParameterTypes().length != 1 ) {
			throw new ServiceDependencyException(
					"Encountered @InjectService on method with unexpected number of parameters"
			);
		}

		Class dependentServiceRole = injectService.serviceRole();
		if ( dependentServiceRole == null || dependentServiceRole.equals( Void.class ) ) {
			dependentServiceRole = injectionMethod.getParameterTypes()[0];
		}

		// todo : because of the use of proxies, this is no longer returning null here...

		final Service dependantService = servicesRegistry.getService( dependentServiceRole );
		if ( dependantService == null ) {
			if ( injectService.required() ) {
				throw new ServiceDependencyException(
						"Dependency [" + dependentServiceRole + "] declared by service [" + service + "] not found"
				);
			}
		}
		else {
			try {
				injectionMethod.invoke( service, dependantService );
			}
			catch ( Exception e ) {
				throw new ServiceDependencyException( "Cannot inject dependency service", e );
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	private <T extends Service> void startService(T service, Class serviceRole) {
		if ( Startable.class.isInstance( service ) ) {
			( (Startable) service ).start();
		}

		if ( Manageable.class.isInstance( service ) ) {
			servicesRegistry.getService( JmxService.class ).registerService( (Manageable) service, serviceRole );
		}
	}

}
