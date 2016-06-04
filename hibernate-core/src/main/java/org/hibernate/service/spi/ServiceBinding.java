/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.spi;

import org.hibernate.service.Service;
import org.hibernate.service.UnknownServiceException;

import org.jboss.logging.Logger;

/**
 * Models a binding for a particular service
 *
 * @author Steve Ebersole
 */
public final class ServiceBinding<R extends Service> {
	private static final Logger log = Logger.getLogger( ServiceBinding.class );

	public interface ServiceLifecycleOwner {
		<R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator);

		<R extends Service> void configureService(R service);

		<R extends Service> void injectDependencies(R service);

		<R extends Service> void startService(R service, Class<R> serviceRole);

		<R extends Service> void stopService(R service);

		default <R extends Service> void registerService(ServiceBinding<R> serviceBinding, R service) {
		}
	}

	private final ServiceLifecycleOwner lifecycleOwner;
	private final Class<R> serviceRole;
	private final ServiceInitiator<R> serviceInitiator;
	private volatile R service;

	public ServiceBinding(ServiceLifecycleOwner lifecycleOwner, Class<R> serviceRole, R service) {
		this.lifecycleOwner = lifecycleOwner;
		this.serviceRole = serviceRole;
		this.serviceInitiator = null;
		this.service = service;
	}

	public ServiceBinding(ServiceLifecycleOwner lifecycleOwner, ServiceInitiator<R> serviceInitiator) {
		this.lifecycleOwner = lifecycleOwner;
		this.serviceRole = serviceInitiator.getServiceInitiated();
		this.serviceInitiator = serviceInitiator;
	}

	public Class<R> getServiceRole() {
		return serviceRole;
	}

	public synchronized R getService() {
		if ( service == null ) {
			if ( serviceInitiator == null ) {
				throw new UnknownServiceException( serviceRole );
			}
			try {
				final R tempService = lifecycleOwner.initiateService( serviceInitiator );

				if ( tempService != null ) {
					lifecycleOwner.injectDependencies( tempService );
					lifecycleOwner.configureService( tempService );
					lifecycleOwner.startService( tempService, serviceRole );
					lifecycleOwner.registerService( this, tempService );
				}

				service = tempService;
			}
			catch (ServiceException e) {
				throw e;
			}
			catch (Exception e) {
				throw new ServiceException(
						"Unable to create requested service [" + serviceRole.getName() + "]",
						e
				);
			}
		}
		return service;
	}

	public synchronized void  stopService(){
		lifecycleOwner.stopService( service );
	}

	public boolean isServiceInstanceOf(Class clazz) {
		return serviceRole.isInstance( clazz );
	}
}
