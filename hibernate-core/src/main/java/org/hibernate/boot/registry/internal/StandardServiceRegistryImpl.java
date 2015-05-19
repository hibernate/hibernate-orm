/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.internal;

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
	private final Map configurationValues;

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
	@SuppressWarnings( {"unchecked"})
	public StandardServiceRegistryImpl(
			BootstrapServiceRegistry bootstrapServiceRegistry,
			List<StandardServiceInitiator> serviceInitiators,
			List<ProvidedService> providedServices,
			Map<?, ?> configurationValues) {
		this( true, bootstrapServiceRegistry, serviceInitiators, providedServices, configurationValues );
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
	@SuppressWarnings( {"unchecked"})
	public StandardServiceRegistryImpl(
			boolean autoCloseRegistry,
			BootstrapServiceRegistry bootstrapServiceRegistry,
			List<StandardServiceInitiator> serviceInitiators,
			List<ProvidedService> providedServices,
			Map<?, ?> configurationValues) {
		super( bootstrapServiceRegistry, autoCloseRegistry );

		this.configurationValues = configurationValues;

		// process initiators
		for ( ServiceInitiator initiator : serviceInitiators ) {
			createServiceBinding( initiator );
		}

		// then, explicitly provided service instances
		for ( ProvidedService providedService : providedServices ) {
			createServiceBinding( providedService );
		}
	}

	@Override
	public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		// todo : add check/error for unexpected initiator types?
		return ( (StandardServiceInitiator<R>) serviceInitiator ).initiateService( configurationValues, this );
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
		if ( Configurable.class.isInstance( serviceBinding.getService() ) ) {
			( (Configurable) serviceBinding.getService() ).configure( configurationValues );
		}
	}
}
