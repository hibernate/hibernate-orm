/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.UnknownServiceException;
import org.hibernate.service.spi.InjectService;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Basic implementation of the {@link ServiceRegistry} and {@link ServiceRegistryImplementor} contracts.
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public abstract class AbstractServiceRegistryImpl
		implements ServiceRegistryImplementor, ServiceBinding.ServiceLifecycleOwner {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractServiceRegistryImpl.class );

	public static final String ALLOW_CRAWLING = "hibernate.service.allow_crawling";

	private volatile @Nullable ServiceRegistryImplementor parent;
	private final boolean allowCrawling;

	private final ConcurrentMap<Class<?>,ServiceBinding<?>> serviceBindingMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<Class<?>,Class<?>> roleXref = new ConcurrentHashMap<>();
	// The services stored in initializedServiceByRole are completely initialized
	// (i.e., configured, dependencies injected, and started)
	private final ConcurrentMap<Class<?>,Service> initializedServiceByRole = new ConcurrentHashMap<>();

	// IMPL NOTE : the list used for ordered destruction.  Cannot used map above because we need to
	// iterate it in reverse order which is only available through ListIterator
	// assume 20 services for initial sizing
	// All access guarded by synchronization on the serviceBindingList itself.
	private final List<ServiceBinding<?>> serviceBindingList = CollectionHelper.arrayList( 20 );

	// Guarded by synchronization on this.
	private final boolean autoCloseRegistry;
	// Guarded by synchronization on this.
	private Set<ServiceRegistryImplementor> childRegistries;

	private final AtomicBoolean active = new AtomicBoolean( true );

	protected final Lock thisLock = new ReentrantLock();
	private final Lock serviceBindingListLock = new ReentrantLock();

	protected AbstractServiceRegistryImpl(@Nullable ServiceRegistryImplementor parent) {
		this( parent, true );
	}

	protected AbstractServiceRegistryImpl(
			@Nullable ServiceRegistryImplementor parent,
			boolean autoCloseRegistry) {

		this.parent = parent;
		this.allowCrawling = ConfigurationHelper.getBoolean( ALLOW_CRAWLING, Environment.getProperties(), true );
		this.autoCloseRegistry = autoCloseRegistry;
	}

	public AbstractServiceRegistryImpl(BootstrapServiceRegistry bootstrapServiceRegistry) {
		this( bootstrapServiceRegistry, true );
	}

	public AbstractServiceRegistryImpl(
			BootstrapServiceRegistry bootstrapServiceRegistry,
			boolean autoCloseRegistry) {

		if ( !(bootstrapServiceRegistry instanceof ServiceRegistryImplementor) ) {
			throw new IllegalArgumentException( "ServiceRegistry parent needs to implement ServiceRegistryImplementor" );
		}
		this.parent = (ServiceRegistryImplementor) bootstrapServiceRegistry;
		this.allowCrawling = ConfigurationHelper.getBoolean( ALLOW_CRAWLING, Environment.getProperties(), true );
		this.autoCloseRegistry = autoCloseRegistry;
	}

	// For nullness checking purposes
	protected void initialize() {
		if ( this.parent != null ) {
			this.parent.registerChild( this );
		}
	}

	protected <R extends Service> void createServiceBinding(ServiceInitiator<R> initiator) {
		final ServiceBinding<?> serviceBinding = new ServiceBinding<>( this, initiator );
		serviceBindingMap.put( initiator.getServiceInitiated(), serviceBinding );
	}

	protected <R extends Service> void createServiceBinding(ProvidedService<R> providedService) {
		ServiceBinding<R> binding = locateServiceBinding( providedService.getServiceRole(), false );
		if ( binding == null ) {
			binding = new ServiceBinding<>( this, providedService.getServiceRole(), providedService.getService() );
			serviceBindingMap.put( providedService.getServiceRole(), binding );
		}
		registerService( binding, providedService.getService() );
	}

	protected void visitServiceBindings(Consumer<ServiceBinding<?>> action) {
		serviceBindingList.forEach( action );
	}

	@Override
	public @Nullable ServiceRegistry getParentServiceRegistry() {
		return parent;
	}

	@Override
	public <R extends Service> @Nullable ServiceBinding<R> locateServiceBinding(Class<R> serviceRole) {
		return locateServiceBinding( serviceRole, true );
	}

	@SuppressWarnings("unchecked")
	protected <R extends Service> @Nullable ServiceBinding<R> locateServiceBinding(Class<R> serviceRole, boolean checkParent) {
		ServiceBinding<R> serviceBinding = (ServiceBinding<R>) serviceBindingMap.get( serviceRole );
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
		final Class<?> alternative = roleXref.get( serviceRole );
		if ( alternative != null ) {
			return (ServiceBinding<R>) serviceBindingMap.get( alternative );
		}

		// perform a crawl looking for an alternate registration
		for ( ServiceBinding<?> binding : serviceBindingMap.values() ) {
			if ( serviceRole.isAssignableFrom( binding.getServiceRole() ) ) {
				// we found an alternate...
				log.alternateServiceRole( serviceRole.getName(), binding.getServiceRole().getName() );
				registerAlternate( serviceRole, binding.getServiceRole() );
				return (ServiceBinding<R>) binding;
			}

			if ( binding.getService() != null && serviceRole.isInstance( binding.getService() ) ) {
				// we found an alternate...
				log.alternateServiceRole( serviceRole.getName(), binding.getServiceRole().getName() );
				registerAlternate( serviceRole, binding.getServiceRole() );
				return (ServiceBinding<R>) binding;
			}
		}

		return null;
	}

	private void registerAlternate(Class<?> alternate, Class<?> target) {
		roleXref.put( alternate, target );
	}

	@Override
	public <R extends Service> @Nullable R getService(Class<R> serviceRole) {
		//Fast-path for ClassLoaderService as it's extremely hot during bootstrap
		//(and after bootstrap service loading performance is less interesting as it's
		//ideally being cached by long term consumers)
		if ( ClassLoaderService.class == serviceRole ) {
			if ( parent != null ) {
				return parent.getService( serviceRole );
			}
		}
		// TODO: should an exception be thrown if active == false???
		R service = serviceRole.cast( initializedServiceByRole.get( serviceRole ) );
		if ( service != null ) {
			return service;
		}

		//Any service initialization needs synchronization
		thisLock.lock();
		try {
			// Check again after having acquired the lock:
			service = serviceRole.cast( initializedServiceByRole.get( serviceRole ) );
			if ( service != null ) {
				return service;
			}

			final ServiceBinding<R> serviceBinding = locateServiceBinding( serviceRole );
			if ( serviceBinding == null ) {
				throw new UnknownServiceException( serviceRole );
			}
			service = serviceBinding.getService();
			if ( service == null ) {
				service = initializeService( serviceBinding );
			}
			if ( service != null ) {
				// add the service only after it is completely initialized
				initializedServiceByRole.put( serviceRole, service );
			}
			return service;
		} finally {
			thisLock.unlock();
		}
	}

	protected <R extends Service> void registerService(ServiceBinding<R> serviceBinding, R service) {
		serviceBinding.setService( service );
		serviceBindingListLock.lock();
		try {
			serviceBindingList.add( serviceBinding );
		} finally {
			serviceBindingListLock.unlock();
		}
	}

	private <R extends Service> @Nullable R initializeService(ServiceBinding<R> serviceBinding) {
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

	protected <R extends Service> @Nullable R createService(ServiceBinding<R> serviceBinding) {
		final ServiceInitiator<R> serviceInitiator = serviceBinding.getServiceInitiator();
		if ( serviceInitiator == null ) {
			// this condition should never ever occur
			throw new UnknownServiceException( serviceBinding.getServiceRole() );
		}

		try {
			R service = serviceBinding.getLifecycleOwner().initiateService( serviceInitiator );
			// IMPL NOTE : the register call here is important to avoid potential stack overflow issues
			//		from recursive calls through #configureService
			if ( service != null ) {
				registerService( serviceBinding, service );
			}
			return service;
		}
		catch ( ServiceException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new ServiceException( "Unable to create requested service ["
					+ serviceBinding.getServiceRole().getName() + "] due to: " + e.getMessage(), e );
		}
	}

	@Override
	public <R extends Service> void injectDependencies(ServiceBinding<R> serviceBinding) {
		final R service = serviceBinding.getService();

		applyInjections( service );

		if ( service instanceof ServiceRegistryAwareService ) {
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

	private <T extends Service> void processInjection(T service, Method injectionMethod, InjectService injectService) {
		final Class<?>[] parameterTypes = injectionMethod.getParameterTypes();
		if ( injectionMethod.getParameterCount() != 1 ) {
			throw new ServiceDependencyException(
					"Encountered @InjectService on method with unexpected number of parameters"
			);
		}

		//noinspection rawtypes
		Class dependentServiceRole = injectService.serviceRole();
		if ( dependentServiceRole == null || dependentServiceRole.equals( Void.class ) ) {
			dependentServiceRole = parameterTypes[0];
		}

		// todo : because of the use of proxies, this is no longer returning null here...

		//noinspection unchecked
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
	public <R extends Service> void startService(ServiceBinding<R> serviceBinding) {
		if ( serviceBinding.getService() instanceof Startable ) {
			( (Startable) serviceBinding.getService() ).start();
		}
	}

	public boolean isActive() {
		return active.get();
	}

	@Override
	public void destroy() {
		thisLock.lock();
		try {
			if ( active.compareAndSet( true, false ) ) {
				try {
					//First thing, make sure that the fast path read is disabled so that
					//threads not owning the synchronization lock can't get an invalid Service:
					initializedServiceByRole.clear();
					serviceBindingListLock.lock();
					try {
						ListIterator<ServiceBinding<?>> serviceBindingsIterator = serviceBindingList.listIterator(
								serviceBindingList.size()
						);
						while ( serviceBindingsIterator.hasPrevious() ) {
							final ServiceBinding<?> serviceBinding = serviceBindingsIterator.previous();
							serviceBinding.getLifecycleOwner().stopService( serviceBinding );
						}
						serviceBindingList.clear();
					} finally {
						serviceBindingListLock.unlock();
					}
					serviceBindingMap.clear();
				}
				finally {
					if ( parent != null ) {
						parent.deRegisterChild( this );
					}
				}
			}
		} finally {
			thisLock.unlock();
		}
	}

	@Override
	public <R extends Service> void stopService(ServiceBinding<R> binding) {
		thisLock.lock();
		try {
			final Service service = binding.getService();
			if ( service instanceof Stoppable ) {
				try {
					( (Stoppable) service ).stop();
				}
				catch ( Exception e ) {
					log.unableToStopService( service.getClass(), e );
				}
			}
		} finally {
			thisLock.unlock();
		}
	}

	@Override
	public void registerChild(ServiceRegistryImplementor child) {
		thisLock.lock();
		try {
			if ( childRegistries == null ) {
				childRegistries = new HashSet<>();
			}
			if ( !childRegistries.add( child ) ) {
				log.warnf(
						"Child ServiceRegistry [%s] was already registered; this will end badly later...",
						child
				);
			}
		} finally {
			thisLock.unlock();
		}
	}

	@Override
	public void deRegisterChild(ServiceRegistryImplementor child) {
		thisLock.lock();
		try {
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
		} finally {
			thisLock.unlock();
		}
	}

	/**
	 * Not intended for general use. We need the ability to stop and "reactivate" a registry to allow
	 * experimentation with technologies such as GraalVM, Quarkus and Cri-O.
	 */
	public void resetParent(@Nullable BootstrapServiceRegistry newParent) {
		thisLock.lock();
		try {
			if ( this.parent != null ) {
				this.parent.deRegisterChild( this );
			}
			if ( newParent != null ) {
				if ( !(newParent instanceof ServiceRegistryImplementor) ) {
					throw new IllegalArgumentException( "ServiceRegistry parent needs to implement ServiceRegistryImplementor" );
				}
				this.parent = (ServiceRegistryImplementor) newParent;
				this.parent.registerChild( this );
			}
			else {
				this.parent = null;
			}
		} finally {
			thisLock.unlock();
		}
	}

	@Override
	public <T extends Service> @Nullable T fromRegistryOrChildren(Class<T> serviceRole) {
		return fromRegistryOrChildren( serviceRole, this, childRegistries );
	}

	public static <T extends Service> @Nullable T fromRegistryOrChildren(
			Class<T> serviceRole,
			ServiceRegistryImplementor serviceRegistry,
			@Nullable Set<ServiceRegistryImplementor> childRegistries) {
		// prefer `serviceRegistry`
		final T localService = serviceRegistry.getService( serviceRole );
		if ( localService != null ) {
			return localService;
		}

		if ( childRegistries != null ) {
			for ( ServiceRegistryImplementor childRegistry : childRegistries ) {
				final T extracted = childRegistry.getService( serviceRole );
				if ( extracted != null ) {
					return extracted;
				}
			}
		}

		return null;
	}

	/**
	 * Not intended for general use. We need the ability to stop and "reactivate" a registry to allow
	 * experimentation with technologies such as GraalVM, Quarkus and Cri-O.
	 */
	public void reactivate() {
		thisLock.lock();
		try {
			if ( !active.compareAndSet( false, true ) ) {
				throw new IllegalStateException( "Was not inactive, could not reactivate" );
			}
		} finally {
			thisLock.unlock();
		}
	}

}
