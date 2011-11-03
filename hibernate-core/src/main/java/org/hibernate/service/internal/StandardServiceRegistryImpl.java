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

import java.util.List;
import java.util.Map;

import org.hibernate.service.BootstrapServiceRegistry;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceInitiator;

/**
 * Hibernate implementation of the standard service registry.
 *
 * @author Steve Ebersole
 */
public class StandardServiceRegistryImpl extends AbstractServiceRegistryImpl implements ServiceRegistry {
	private final Map configurationValues;

	@SuppressWarnings( {"unchecked"})
	public StandardServiceRegistryImpl(
			BootstrapServiceRegistry bootstrapServiceRegistry,
			List<BasicServiceInitiator> serviceInitiators,
			List<ProvidedService> providedServices,
			Map<?, ?> configurationValues) {
		super( bootstrapServiceRegistry );

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
		return ( (BasicServiceInitiator<R>) serviceInitiator ).initiateService( configurationValues, this );
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
		if ( Configurable.class.isInstance( serviceBinding.getService() ) ) {
			( (Configurable) serviceBinding.getService() ).configure( configurationValues );
		}
	}

}
