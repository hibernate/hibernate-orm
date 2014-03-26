/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.registry.internal;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.internal.StrategySelectorImpl;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.integrator.internal.IntegratorServiceImpl;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

/**
 * {@link ServiceRegistry} implementation containing specialized "bootstrap" services, specifically:<ul>
 *     <li>{@link ClassLoaderService}</li>
 *     <li>{@link IntegratorService}</li>
 *     <li>{@link StrategySelector}</li>
 * </ul>
 *
 * IMPL NOTE : Currently implements the deprecated {@link org.hibernate.service.BootstrapServiceRegistry} contract
 * so that the registry returned from the builder works on the deprecated sense.  Once
 * {@link org.hibernate.service.BootstrapServiceRegistry} goes away, this should be updated to instead implement
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistry}.
 *
 * @author Steve Ebersole
 */
public class BootstrapServiceRegistryImpl
		implements ServiceRegistryImplementor, BootstrapServiceRegistry, ServiceBinding.ServiceLifecycleOwner {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( BootstrapServiceRegistryImpl.class );

	private final boolean autoCloseRegistry;
	private boolean active = true;

	private static final LinkedHashSet<Integrator> NO_INTEGRATORS = new LinkedHashSet<Integrator>();

	private final ServiceBinding<ClassLoaderService> classLoaderServiceBinding;
	private final ServiceBinding<StrategySelector> strategySelectorBinding;
	private final ServiceBinding<IntegratorService> integratorServiceBinding;

	private Set<ServiceRegistryImplementor> childRegistries;

	/**
	 * Constructs a BootstrapServiceRegistryImpl.
	 *
	 * Do not use directly generally speaking.  Use {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder}
	 * instead.
	 *
	 * @see org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
	 */
	public BootstrapServiceRegistryImpl() {
		this( new ClassLoaderServiceImpl(), NO_INTEGRATORS );
	}
	/**
	 * Constructs a BootstrapServiceRegistryImpl.
	 *
	 * Do not use directly generally speaking.  Use {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder}
	 * instead.
	 *
	 * @param classLoaderService The ClassLoaderService to use
	 * @param providedIntegrators The group of explicitly provided integrators
	 *
	 * @see org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
	 */
	public BootstrapServiceRegistryImpl(
			ClassLoaderService classLoaderService,
			LinkedHashSet<Integrator> providedIntegrators) {
		this( true, classLoaderService, providedIntegrators );
	}

	/**
	 * Constructs a BootstrapServiceRegistryImpl.
	 *
	 * Do not use directly generally speaking.  Use {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder}
	 * instead.
	 *
	 * @param autoCloseRegistry See discussion on
	 * {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder#disableAutoClose}
	 * @param classLoaderService The ClassLoaderService to use
	 * @param providedIntegrators The group of explicitly provided integrators
	 *
	 * @see org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
	 */
	public BootstrapServiceRegistryImpl(
			boolean autoCloseRegistry,
			ClassLoaderService classLoaderService,
			LinkedHashSet<Integrator> providedIntegrators) {
		this.autoCloseRegistry = autoCloseRegistry;

		this.classLoaderServiceBinding = new ServiceBinding<ClassLoaderService>(
				this,
				ClassLoaderService.class,
				classLoaderService
		);

		final StrategySelectorImpl strategySelector = new StrategySelectorImpl( classLoaderService );
		this.strategySelectorBinding = new ServiceBinding<StrategySelector>(
				this,
				StrategySelector.class,
				strategySelector
		);

		this.integratorServiceBinding = new ServiceBinding<IntegratorService>(
				this,
				IntegratorService.class,
				new IntegratorServiceImpl( providedIntegrators, classLoaderService )
		);
	}


	/**
	 * Constructs a BootstrapServiceRegistryImpl.
	 *
	 * Do not use directly generally speaking.  Use {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder}
	 * instead.
	 *
	 * @param classLoaderService The ClassLoaderService to use
	 * @param strategySelector The StrategySelector to use
	 * @param integratorService The IntegratorService to use
	 *
	 * @see org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
	 */
	public BootstrapServiceRegistryImpl(
			ClassLoaderService classLoaderService,
			StrategySelector strategySelector,
			IntegratorService integratorService) {
		this( true, classLoaderService, strategySelector, integratorService );
	}


	/**
	 * Constructs a BootstrapServiceRegistryImpl.
	 *
	 * Do not use directly generally speaking.  Use {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder}
	 * instead.
	 *
	 * @param autoCloseRegistry See discussion on
	 * {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder#disableAutoClose}
	 * @param classLoaderService The ClassLoaderService to use
	 * @param strategySelector The StrategySelector to use
	 * @param integratorService The IntegratorService to use
	 *
	 * @see org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
	 */
	public BootstrapServiceRegistryImpl(
			boolean autoCloseRegistry,
			ClassLoaderService classLoaderService,
			StrategySelector strategySelector,
			IntegratorService integratorService) {
		this.autoCloseRegistry = autoCloseRegistry;

		this.classLoaderServiceBinding = new ServiceBinding<ClassLoaderService>(
				this,
				ClassLoaderService.class,
				classLoaderService
		);

		this.strategySelectorBinding = new ServiceBinding<StrategySelector>(
				this,
				StrategySelector.class,
				strategySelector
		);

		this.integratorServiceBinding = new ServiceBinding<IntegratorService>(
				this,
				IntegratorService.class,
				integratorService
		);
	}



	@Override
	public <R extends Service> R getService(Class<R> serviceRole) {
		final ServiceBinding<R> binding = locateServiceBinding( serviceRole );
		return binding == null ? null : binding.getService();
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <R extends Service> ServiceBinding<R> locateServiceBinding(Class<R> serviceRole) {
		if ( ClassLoaderService.class.equals( serviceRole ) ) {
			return (ServiceBinding<R>) classLoaderServiceBinding;
		}
		else if ( StrategySelector.class.equals( serviceRole) ) {
			return (ServiceBinding<R>) strategySelectorBinding;
		}
		else if ( IntegratorService.class.equals( serviceRole ) ) {
			return (ServiceBinding<R>) integratorServiceBinding;
		}

		return null;
	}

	@Override
	public void destroy() {
		if ( !active ) {
			return;
		}
		active = false;
		destroy( classLoaderServiceBinding );
		destroy( strategySelectorBinding );
		destroy( integratorServiceBinding );
	}
	
	private void destroy(ServiceBinding serviceBinding) {
		serviceBinding.getLifecycleOwner().stopService( serviceBinding );
	}

	public boolean isActive() {
		return active;
	}

	@Override
	public ServiceRegistry getParentServiceRegistry() {
		return null;
	}

	@Override
	public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		throw new ServiceException( "Boot-strap registry should only contain provided services" );
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> binding) {
		throw new ServiceException( "Boot-strap registry should only contain provided services" );
	}

	@Override
	public <R extends Service> void injectDependencies(ServiceBinding<R> binding) {
		throw new ServiceException( "Boot-strap registry should only contain provided services" );
	}

	@Override
	public <R extends Service> void startService(ServiceBinding<R> binding) {
		throw new ServiceException( "Boot-strap registry should only contain provided services" );
	}

	@Override
	public <R extends Service> void stopService(ServiceBinding<R> binding) {
		final Service service = binding.getService();
		if ( Stoppable.class.isInstance( service ) ) {
			try {
				( (Stoppable) service ).stop();
			}
			catch ( Exception e ) {
				LOG.unableToStopService( service.getClass(), e.toString() );
			}
		}
	}

	@Override
	public void registerChild(ServiceRegistryImplementor child) {
		if ( childRegistries == null ) {
			childRegistries = new HashSet<ServiceRegistryImplementor>();
		}
		if ( !childRegistries.add( child ) ) {
			LOG.warnf(
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
				LOG.debug(
						"Implicitly destroying Boot-strap registry on de-registration " +
								"of all child ServiceRegistries"
				);
				destroy();
			}
			else {
				LOG.debug(
						"Skipping implicitly destroying Boot-strap registry on de-registration " +
								"of all child ServiceRegistries"
				);
			}
		}
	}
}
