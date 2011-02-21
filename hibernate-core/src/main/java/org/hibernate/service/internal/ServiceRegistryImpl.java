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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.HibernateException;
import org.hibernate.HibernateLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.internal.proxy.javassist.ServiceProxyFactoryFactoryImpl;
import org.hibernate.service.spi.Service;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.StandardServiceInitiators;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.service.spi.UnknownServiceException;
import org.hibernate.service.spi.proxy.ServiceProxyFactory;
import org.hibernate.service.spi.proxy.ServiceProxyTargetSource;
import org.hibernate.util.CollectionHelper;
import org.jboss.logging.Logger;

/**
 * Standard Hibernate implementation of the service registry.
 *
 * @author Steve Ebersole
 */
public class ServiceRegistryImpl implements ServiceProxyTargetSource {

    private static final HibernateLogger LOG = Logger.getMessageLogger(HibernateLogger.class, ServiceRegistryImpl.class.getName());

	private final ServiceInitializer initializer;
	// for now just hardcode the javassist factory
	private ServiceProxyFactory serviceProxyFactory = new ServiceProxyFactoryFactoryImpl().makeServiceProxyFactory( this );

	private ConcurrentHashMap<Class,ServiceBinding> serviceBindingMap;
	// IMPL NOTE : the list used for ordered destruction.  Cannot used ordered map above because we need to
	// iterate it in reverse order which is only available through ListIterator
	private List<Service> serviceList = new ArrayList<Service>();

	public ServiceRegistryImpl(Map configurationValues) {
		this( StandardServiceInitiators.LIST, configurationValues );
	}

	public ServiceRegistryImpl(List<ServiceInitiator> serviceInitiators, Map configurationValues) {
		this.initializer = new ServiceInitializer( this, serviceInitiators, ConfigurationHelper.clone( configurationValues ) );
		final int anticipatedSize = serviceInitiators.size() + 5; // allow some growth
		serviceBindingMap = CollectionHelper.concurrentMap( anticipatedSize );
		serviceList = CollectionHelper.arrayList( anticipatedSize );
	}

	public void destroy() {
		ListIterator<Service> serviceIterator = serviceList.listIterator();
		while ( serviceIterator.hasPrevious() ) {
			final Service service = serviceIterator.previous();
			if ( Stoppable.class.isInstance( service ) ) {
				try {
					( (Stoppable) service ).stop();
				}
				catch ( Exception e ) {
                    LOG.unableToStopService(service.getClass(), e.toString());
				}
			}
		}
		serviceList.clear();
		serviceList = null;
		serviceBindingMap.clear();
		serviceBindingMap = null;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <T extends Service> T getService(Class<T> serviceRole) {
		return locateOrCreateServiceBinding( serviceRole ).getProxy();
	}

	@SuppressWarnings({ "unchecked" })
	private <T extends Service> ServiceBinding<T> locateOrCreateServiceBinding(Class<T> serviceRole) {
		ServiceBinding<T> serviceBinding = serviceBindingMap.get( serviceRole );
		if ( serviceBinding == null ) {
			T proxy = serviceProxyFactory.makeProxy( serviceRole );
			serviceBinding = new ServiceBinding<T>( proxy );
			serviceBindingMap.put( serviceRole, serviceBinding );
		}
		return serviceBinding;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T extends Service> T getServiceInternal(Class<T> serviceRole) {
		ServiceBinding<T> serviceBinding = serviceBindingMap.get( serviceRole );
		if ( serviceBinding == null ) {
			throw new HibernateException( "Only proxies should invoke #getServiceInternal" );
		}
		T service = serviceBinding.getTarget();
		if ( service == null ) {
			service = initializer.initializeService( serviceRole );
			serviceBinding.setTarget( service );
		}
		if ( service == null ) {
			throw new UnknownServiceException( serviceRole );
		}
		return service;
	}

	@Override
	public <T extends Service> void registerService(Class<T> serviceRole, T service) {
		ServiceBinding<T> serviceBinding = locateOrCreateServiceBinding( serviceRole );
		T priorServiceInstance = serviceBinding.getTarget();
		serviceBinding.setTarget( service );
		if ( priorServiceInstance != null ) {
			serviceList.remove( priorServiceInstance );
		}
		serviceList.add( service );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public void registerServiceInitiator(ServiceInitiator initiator) {
		ServiceBinding serviceBinding = serviceBindingMap.get( initiator.getServiceInitiated() );
		if ( serviceBinding != null ) {
			serviceBinding.setTarget( null );
		}
		initializer.registerServiceInitiator( initiator );
	}

	private static final class ServiceBinding<T> {
		private final T proxy;
		private T target;

		private ServiceBinding(T proxy) {
			this.proxy = proxy;
		}

		public T getProxy() {
			return proxy;
		}

		public T getTarget() {
			return target;
		}

		public void setTarget(T target) {
			this.target = target;
		}
	}
}
