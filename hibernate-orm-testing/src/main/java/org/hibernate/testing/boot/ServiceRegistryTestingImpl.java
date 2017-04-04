/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.boot;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.StandardServiceInitiators;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.env.ConnectionProviderBuilder;

/**
 * ServiceRegistry useful in testing
 *
 * @author Steve Ebersole
 */
public class ServiceRegistryTestingImpl
		extends StandardServiceRegistryImpl
		implements ServiceRegistryImplementor {

	public static ServiceRegistryTestingImpl forUnitTesting() {
		return new ServiceRegistryTestingImpl(
				true,
				new BootstrapServiceRegistryBuilder().build(),
				StandardServiceInitiators.LIST,
				Arrays.asList(
						dialectFactoryService(),
						connectionProviderService()
				),
				Environment.getProperties()
		);
	}

	public static ServiceRegistryTestingImpl forUnitTesting(Map settings) {
		return new ServiceRegistryTestingImpl(
				true,
				new BootstrapServiceRegistryBuilder().build(),
				StandardServiceInitiators.LIST,
				Arrays.asList(
						dialectFactoryService(),
						connectionProviderService()
				),
				settings
		);
	}

	private static ProvidedService dialectFactoryService() {
		return new ProvidedService<DialectFactory>( DialectFactory.class, new DialectFactoryTestingImpl() );
	}

	private static ProvidedService connectionProviderService() {
		return new ProvidedService<ConnectionProvider>(
				ConnectionProvider.class,
				ConnectionProviderBuilder.buildConnectionProvider( true )
		);
	}

	public ServiceRegistryTestingImpl(
			boolean autoCloseRegistry,
			BootstrapServiceRegistry bootstrapServiceRegistry,
			List<StandardServiceInitiator> serviceInitiators,
			List<ProvidedService> providedServices,
			Map<?, ?> configurationValues) {
		super( autoCloseRegistry, bootstrapServiceRegistry, serviceInitiators, providedServices, configurationValues );
	}
}
