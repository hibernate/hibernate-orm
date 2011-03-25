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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.HibernateLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.internal.proxy.javassist.ServiceProxyFactoryFactoryImpl;
import org.hibernate.service.jmx.spi.JmxService;
import org.hibernate.service.spi.InjectService;
import org.hibernate.service.spi.Manageable;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.service.UnknownServiceException;
import org.hibernate.service.spi.proxy.ServiceProxyFactory;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractServiceRegistryImpl implements ServiceRegistryImplementor {
	private static final HibernateLogger LOG = Logger.getMessageLogger( HibernateLogger.class, AbstractServiceRegistryImpl.class.getName() );

	private final ServiceRegistryImplementor parent;

	// for now just hard-code the javassist factory
	private ServiceProxyFactory serviceProxyFactory = new ServiceProxyFactoryFactoryImpl().makeServiceProxyFactory( this );

	private ConcurrentHashMap<Class,ServiceBinding> serviceBindingMap;
	// IMPL NOTE : the list used for ordered destruction.  Cannot used map above because we need to
	// iterate it in reverse order which is only available through ListIterator
	private List<Service> serviceList = new ArrayList<Service>();

	protected AbstractServiceRegistryImpl() {
		this( null );
	}

	protected AbstractServiceRegistryImpl(ServiceRegistryImplementor parent) {
		this.parent = parent;
		// assume 20 services for initial sizing
		this.serviceBindingMap = CollectionHelper.concurrentMap( 20 );
		this.serviceList = CollectionHelper.arrayList( 20 );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public ServiceRegistry getParentServiceRegistry() {
		return parent;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R extends Service> ServiceBinding<R> locateServiceBinding(Class<R> serviceRole) {
		return locateServiceBinding( serviceRole, true );
	}

	@SuppressWarnings({ "unchecked" })
	protected <R extends Service> ServiceBinding<R> locateServiceBinding(Class<R> serviceRole, boolean checkParent) {
		ServiceBinding<R> serviceBinding = serviceBindingMap.get( serviceRole );
		if ( serviceBinding == null && checkParent && parent != null ) {
			// look in parent
			serviceBinding = parent.locateServiceBinding( serviceRole );
		}
		return serviceBinding;
	}

	@Override
	public <R extends Service> R getService(Class<R> serviceRole) {
		return locateOrCreateServiceBinding( serviceRole, true ).getProxy();
	}

	@SuppressWarnings({ "unchecked" })
	protected <R extends Service> ServiceBinding<R> locateOrCreateServiceBinding(Class<R> serviceRole, boolean checkParent) {
		ServiceBinding<R> serviceBinding = locateServiceBinding( serviceRole, checkParent );
		if ( serviceBinding == null ) {
			R proxy = serviceProxyFactory.makeProxy( serviceRole );
			serviceBinding = new ServiceBinding<R>( proxy );
			serviceBindingMap.put( serviceRole, serviceBinding );
		}
		return serviceBinding;
	}

	@Override
	public <R extends Service> void registerService(Class<R> serviceRole, R service) {
		ServiceBinding<R> serviceBinding = locateOrCreateServiceBinding( serviceRole, false );
		R priorServiceInstance = serviceBinding.getTarget();
		serviceBinding.setTarget( service );
		if ( priorServiceInstance != null ) {
			serviceList.remove( priorServiceInstance );
		}
		serviceList.add( service );
	}

	private <R extends Service> R initializeService(Class<R> serviceRole) {
        LOG.trace("Initializing service [role=" + serviceRole.getName() + "]");

		// PHASE 1 : create service
		R service = createService( serviceRole );
		if ( service == null ) {
			return null;
		}

		// PHASE 2 : configure service (***potentially recursive***)
		configureService( service );

		// PHASE 3 : Start service
		startService( service, serviceRole );

		return service;
	}

	protected abstract <T extends Service> T createService(Class<T> serviceRole);
	protected abstract <T extends Service> void configureService(T service);

	protected <T extends Service> void applyInjections(T service) {
		try {
			for ( Method method : service.getClass().getMethods() ) {
				InjectService injectService = method.getAnnotation( InjectService.class );
				if ( injectService == null ) {
					continue;
				}

				applyInjection( service, method, injectService );
			}
		}
		catch (NullPointerException e) {
            LOG.error("NPE injecting service deps : " + service.getClass().getName());
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

		final Service dependantService = getService( dependentServiceRole );
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
	protected <T extends Service> void startService(T service, Class serviceRole) {
		if ( Startable.class.isInstance( service ) ) {
			( (Startable) service ).start();
		}

		if ( Manageable.class.isInstance( service ) ) {
			getService( JmxService.class ).registerService( (Manageable) service, serviceRole );
		}
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <R extends Service> R getServiceInternal(Class<R> serviceRole) {
		// this call comes from the binding proxy, we most definitely do not want to look up into the parent
		// in this case!
		ServiceBinding<R> serviceBinding = locateServiceBinding( serviceRole, false );
		if ( serviceBinding == null ) {
			throw new HibernateException( "Only proxies should invoke #getServiceInternal" );
		}
		R service = serviceBinding.getTarget();
		if ( service == null ) {
			service = initializeService( serviceRole );
			serviceBinding.setTarget( service );
		}
		if ( service == null ) {
			throw new UnknownServiceException( serviceRole );
		}
		return service;
	}

	public void destroy() {
		ListIterator<Service> serviceIterator = serviceList.listIterator( serviceList.size() );
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

}
