/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.service.Service;
import org.hibernate.service.internal.AbstractServiceRegistryImpl;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceInitiator;

/**
 * Standard Hibernate implementation of the standard service registry.
 *
 * @author Steve Ebersole
 */
public class StandardServiceRegistryImpl extends AbstractServiceRegistryImpl implements StandardServiceRegistry {

	//Access to this field requires synchronization on -this-
	private Map<String,Object> configurationValues;

	protected StandardServiceRegistryImpl(
			boolean autoCloseRegistry,
			BootstrapServiceRegistry bootstrapServiceRegistry,
			Map<String,Object> configurationValues) {
		super( bootstrapServiceRegistry, autoCloseRegistry );
		this.configurationValues = configurationValues;
	}

	/**
	 * Constructs a StandardServiceRegistryImpl.  Should not be instantiated directly; use
	 * {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder} instead
	 *
	 * @param bootstrapServiceRegistry The bootstrap service registry.
	 * @param serviceInitiators Any StandardServiceInitiators provided by the user to the builder
	 * @param providedServices Any standard services provided directly to the builder
	 * @param configurationValues Configuration values
	 *
	 * @see org.hibernate.boot.registry.StandardServiceRegistryBuilder
	 */
	public static StandardServiceRegistryImpl create(
			BootstrapServiceRegistry bootstrapServiceRegistry,
			List<StandardServiceInitiator<?>> serviceInitiators,
			List<ProvidedService<?>> providedServices,
			Map<String,Object> configurationValues) {

		return create( true, bootstrapServiceRegistry, serviceInitiators, providedServices, configurationValues );
	}

	/**
	 * Constructs a StandardServiceRegistryImpl.  Should not be instantiated directly; use
	 * {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder} instead
	 *
	 * @param autoCloseRegistry See discussion on
	 * {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder#disableAutoClose}
	 * @param bootstrapServiceRegistry The bootstrap service registry.
	 * @param serviceInitiators Any StandardServiceInitiators provided by the user to the builder
	 * @param providedServices Any standard services provided directly to the builder
	 * @param configurationValues Configuration values
	 *
	 * @see org.hibernate.boot.registry.StandardServiceRegistryBuilder
	 */
	public static StandardServiceRegistryImpl create(
			boolean autoCloseRegistry,
			BootstrapServiceRegistry bootstrapServiceRegistry,
			List<StandardServiceInitiator<?>> serviceInitiators,
			List<ProvidedService<?>> providedServices,
			Map<String,Object> configurationValues) {

		StandardServiceRegistryImpl instance = new StandardServiceRegistryImpl( autoCloseRegistry, bootstrapServiceRegistry, configurationValues );
		instance.initialize();
		instance.applyServiceRegistrations( serviceInitiators, providedServices );

		return instance;
	}

	protected void applyServiceRegistrations(List<StandardServiceInitiator<?>> serviceInitiators, List<ProvidedService<?>> providedServices) {
		try {
			// process initiators
			for ( ServiceInitiator<?> initiator : serviceInitiators ) {
				createServiceBinding( initiator );
			}

			// then, explicitly provided service instances
			//noinspection rawtypes
			for ( ProvidedService providedService : providedServices ) {
				//noinspection unchecked
				createServiceBinding( providedService );
			}
		}
		catch (RuntimeException e) {
			visitServiceBindings( binding -> binding.getLifecycleOwner().stopService( binding ) );
			throw e;
		}
	}

	/**
	 * Not intended for general use. We need the ability to stop and "reactivate" a registry to allow
	 * experimentation with technologies such as GraalVM, Quarkus and Cri-O.
	 */
	public synchronized void resetAndReactivate(BootstrapServiceRegistry bootstrapServiceRegistry,
												List<StandardServiceInitiator<?>> serviceInitiators,
												List<ProvidedService<?>> providedServices,
												Map<?, ?> configurationValues) {
		if ( super.isActive() ) {
			throw new IllegalStateException( "Can't reactivate an active registry" );
		}
		super.resetParent( bootstrapServiceRegistry );
		this.configurationValues = new HashMap( configurationValues );
		super.reactivate();
		applyServiceRegistrations( serviceInitiators, providedServices );
	}


	@Override
	public synchronized <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		// todo : add check/error for unexpected initiator types?
		return ( (StandardServiceInitiator<R>) serviceInitiator ).initiateService( configurationValues, this );
	}

	@Override
	public synchronized <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
		if ( serviceBinding.getService() instanceof Configurable ) {
			( (Configurable) serviceBinding.getService() ).configure( configurationValues );
		}
	}

	@Override
	public synchronized void destroy() {
		super.destroy();
		this.configurationValues = null;
	}
}
