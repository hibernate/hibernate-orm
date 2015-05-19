/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.internal;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jmx.spi.JmxService;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.UnknownServiceException;
import org.hibernate.service.spi.InjectService;
import org.hibernate.service.spi.Manageable;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * Basic implementation of the ServiceRegistry and ServiceRegistryImplementor contracts
 *
 * @author Steve Ebersole
 */
public abstract class AbstractServiceRegistryImpl
		implements ServiceRegistryImplementor, ServiceBinding.ServiceLifecycleOwner {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractServiceRegistryImpl.class );

	public static final String ALLOW_CRAWLING = "hibernate.service.allow_crawling";

	private final ServiceRegistryImplementor parent;
	private final boolean allowCrawling;

	private final ConcurrentServiceBinding<Class,ServiceBinding> serviceBindingMap = new ConcurrentServiceBinding<Class,ServiceBinding>();
	private ConcurrentServiceBinding<Class,Class> roleXref;

	// IMPL NOTE : the list used for ordered destruction.  Cannot used map above because we need to
	// iterate it in reverse order which is only available through ListIterator
	// assume 20 services for initial sizing
	private final List<ServiceBinding> serviceBindingList = CollectionHelper.arrayList( 20 );

	private boolean autoCloseRegistry;
	private Set<ServiceRegistryImplementor> childRegistries;

	@SuppressWarnings( {"UnusedDeclaration"})
	protected AbstractServiceRegistryImpl() {
		this( (ServiceRegistryImplementor) null );
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	protected AbstractServiceRegistryImpl(boolean autoCloseRegistry) {
		this( (ServiceRegistryImplementor) null, autoCloseRegistry );
	}

	protected AbstractServiceRegistryImpl(ServiceRegistryImplementor parent) {
		this( parent, true );
	}

	protected AbstractServiceRegistryImpl(
			ServiceRegistryImplementor parent,
			boolean autoCloseRegistry) {
		this.parent = parent;
		this.allowCrawling = ConfigurationHelper.getBoolean( ALLOW_CRAWLING, Environment.getProperties(), true );

		this.autoCloseRegistry = autoCloseRegistry;
		this.parent.registerChild( this );
	}

	public AbstractServiceRegistryImpl(BootstrapServiceRegistry bootstrapServiceRegistry) {
		this( bootstrapServiceRegistry, true );
	}

	public AbstractServiceRegistryImpl(
			BootstrapServiceRegistry bootstrapServiceRegistry,
			boolean autoCloseRegistry) {
		if ( ! ServiceRegistryImplementor.class.isInstance( bootstrapServiceRegistry ) ) {
			throw new IllegalArgumentException( "ServiceRegistry parent needs to implement ServiceRegistryImplementor" );
		}
		this.parent = (ServiceRegistryImplementor) bootstrapServiceRegistry;
		this.allowCrawling = ConfigurationHelper.getBoolean( ALLOW_CRAWLING, Environment.getProperties(), true );

		this.autoCloseRegistry = autoCloseRegistry;
		this.parent.registerChild( this );
	}

	@SuppressWarnings({ "unchecked" })
	protected <R extends Service> void createServiceBinding(ServiceInitiator<R> initiator) {
		final ServiceBinding serviceBinding = new ServiceBinding( this, initiator );
		serviceBindingMap.put( initiator.getServiceInitiated(), serviceBinding );
	}

	protected <R extends Service> void createServiceBinding(ProvidedService<R> providedService) {
		ServiceBinding<R> binding = locateServiceBinding( providedService.getServiceRole(), false );
		if ( binding == null ) {
			binding = new ServiceBinding<R>( this, providedService.getServiceRole(), providedService.getService() );
			serviceBindingMap.put( providedService.getServiceRole(), binding );
		}
		registerService( binding, providedService.getService() );
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

		if ( serviceBinding != null ) {
			return serviceBinding;
		}

		if ( !allowCrawling ) {
			return null;
		}

		// look for a previously resolved alternate registration
		if ( roleXref != null ) {
			final Class alternative = roleXref.get( serviceRole );
			if ( alternative != null ) {
				return serviceBindingMap.get( alternative );
			}
		}

		// perform a crawl looking for an alternate registration
		for ( ServiceBinding binding : serviceBindingMap.values() ) {
			if ( serviceRole.isAssignableFrom( binding.getServiceRole() ) ) {
				// we found an alternate...
				log.alternateServiceRole( serviceRole.getName(), binding.getServiceRole().getName() );
				registerAlternate( serviceRole, binding.getServiceRole() );
				return binding;
			}

			if ( binding.getService() != null && serviceRole.isInstance( binding.getService() ) ) {
				// we found an alternate...
				log.alternateServiceRole( serviceRole.getName(), binding.getServiceRole().getName() );
				registerAlternate( serviceRole, binding.getServiceRole() );
				return binding;
			}
		}

		return null;
	}

	private void registerAlternate(Class alternate, Class target) {
		if ( roleXref == null ) {
			roleXref = new ConcurrentServiceBinding<Class,Class>();
		}
		roleXref.put( alternate, target );
	}

	@Override
	public <R extends Service> R getService(Class<R> serviceRole) {
		final ServiceBinding<R> serviceBinding = locateServiceBinding( serviceRole );
		if ( serviceBinding == null ) {
			throw new UnknownServiceException( serviceRole );
		}

		R service = serviceBinding.getService();
		if ( service == null ) {
			service = initializeService( serviceBinding );
		}

		return service;
	}

	protected <R extends Service> void registerService(ServiceBinding<R> serviceBinding, R service) {
		serviceBinding.setService( service );
		synchronized ( serviceBindingList ) {
			serviceBindingList.add( serviceBinding );
		}
	}

	private <R extends Service> R initializeService(ServiceBinding<R> serviceBinding) {
		if ( log.isTraceEnabled() ) {
			log.tracev( "Initializing service [role={0}]", serviceBinding.getServiceRole().getName() );
		}

		// PHASE 1 : create service
		R service = createService( serviceBinding );
		if ( service == null ) {
			return null;
		}

		// PHASE 2 : inject service (***potentially recursive***)
		serviceBinding.getLifecycleOwner().injectDependencies( serviceBinding );

		// PHASE 3 : configure service
		serviceBinding.getLifecycleOwner().configureService( serviceBinding );

		// PHASE 4 : Start service
		serviceBinding.getLifecycleOwner().startService( serviceBinding );

		return service;
	}

	@SuppressWarnings( {"unchecked"})
	protected <R extends Service> R createService(ServiceBinding<R> serviceBinding) {
		final ServiceInitiator<R> serviceInitiator = serviceBinding.getServiceInitiator();
		if ( serviceInitiator == null ) {
			// this condition should never ever occur
			throw new UnknownServiceException( serviceBinding.getServiceRole() );
		}

		try {
			R service = serviceBinding.getLifecycleOwner().initiateService( serviceInitiator );
			// IMPL NOTE : the register call here is important to avoid potential stack overflow issues
			//		from recursive calls through #configureService
			registerService( serviceBinding, service );
			return service;
		}
		catch ( ServiceException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new ServiceException( "Unable to create requested service [" + serviceBinding.getServiceRole().getName() + "]", e );
		}
	}

	@Override
	public <R extends Service> void injectDependencies(ServiceBinding<R> serviceBinding) {
		final R service = serviceBinding.getService();

		applyInjections( service );

		if ( ServiceRegistryAwareService.class.isInstance( service ) ) {
			( (ServiceRegistryAwareService) service ).injectServices( this );
		}
	}

	private <R extends Service> void applyInjections(R service) {
		try {
			for ( Method method : service.getClass().getMethods() ) {
				InjectService injectService = method.getAnnotation( InjectService.class );
				if ( injectService == null ) {
					continue;
				}

				processInjection( service, method, injectService );
			}
		}
		catch (NullPointerException e) {
			log.error( "NPE injecting service deps : " + service.getClass().getName() );
		}
	}

	@SuppressWarnings({ "unchecked" })
	private <T extends Service> void processInjection(T service, Method injectionMethod, InjectService injectService) {
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

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R extends Service> void startService(ServiceBinding<R> serviceBinding) {
		if ( Startable.class.isInstance( serviceBinding.getService() ) ) {
			( (Startable) serviceBinding.getService() ).start();
		}

		if ( Manageable.class.isInstance( serviceBinding.getService() ) ) {
			getService( JmxService.class ).registerService(
					(Manageable) serviceBinding.getService(),
					serviceBinding.getServiceRole()
			);
		}
	}

	private boolean active = true;

	public boolean isActive() {
		return active;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public void destroy() {
		if ( !active ) {
			return;
		}

		active = false;
		try {
			synchronized (serviceBindingList) {
				ListIterator<ServiceBinding> serviceBindingsIterator = serviceBindingList.listIterator(
						serviceBindingList.size()
				);
				while ( serviceBindingsIterator.hasPrevious() ) {
					final ServiceBinding serviceBinding = serviceBindingsIterator.previous();
					serviceBinding.getLifecycleOwner().stopService( serviceBinding );
				}
				serviceBindingList.clear();
			}
			serviceBindingMap.clear();
		}
		finally {
			parent.deRegisterChild( this );
		}
	}

	@Override
	public <R extends Service> void stopService(ServiceBinding<R> binding) {
		final Service service = binding.getService();
		if ( Stoppable.class.isInstance( service ) ) {
			try {
				( (Stoppable) service ).stop();
			}
			catch ( Exception e ) {
				log.unableToStopService( service.getClass(), e.toString() );
			}
		}
	}

	@Override
	public void registerChild(ServiceRegistryImplementor child) {
		if ( childRegistries == null ) {
			childRegistries = new HashSet<ServiceRegistryImplementor>();
		}
		if ( !childRegistries.add( child ) ) {
			log.warnf(
					"Child ServiceRegistry [%s] was already registered; this will end badly later...",
					child
			);
		}
	}

	@Override
	public void deRegisterChild(ServiceRegistryImplementor child) {
		if ( childRegistries == null ) {
			throw new IllegalStateException( "No child ServiceRegistry registrations found" );
		}
		childRegistries.remove( child );
		if ( childRegistries.isEmpty() ) {
			if ( autoCloseRegistry ) {
				log.debug(
						"Implicitly destroying ServiceRegistry on de-registration " +
								"of all child ServiceRegistries"
				);
				destroy();
			}
			else {
				log.debug(
						"Skipping implicitly destroying ServiceRegistry on de-registration " +
								"of all child ServiceRegistries"
				);
			}
		}
	}
}
