/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.internal.util.PropertiesHelper;
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

	private ServiceRegistryTestingImpl(
			boolean autoCloseRegistry,
			BootstrapServiceRegistry bootstrapServiceRegistry,
			Map<String, Object> configurationValues) {
		super( autoCloseRegistry, bootstrapServiceRegistry, configurationValues );
	}

	public static ServiceRegistryTestingImpl forUnitTesting() {
		return ServiceRegistryTestingImpl.create(
				true,
				new BootstrapServiceRegistryBuilder().build(),
				StandardServiceInitiators.LIST,
				Arrays.asList(
						dialectFactoryService(),
						connectionProviderService()
				),
				PropertiesHelper.map( Environment.getProperties() )
		);
	}

	public static ServiceRegistryTestingImpl forUnitTesting(Map<String,Object> settings) {
		return ServiceRegistryTestingImpl.create(
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

	private static ProvidedService<DialectFactory> dialectFactoryService() {
		return new ProvidedService<>( DialectFactory.class, new DialectFactoryTestingImpl() );
	}

	private static ProvidedService<ConnectionProvider> connectionProviderService() {
		return new ProvidedService<>(
				ConnectionProvider.class,
				ConnectionProviderBuilder.buildConnectionProvider( true )
		);
	}

	public static ServiceRegistryTestingImpl create(
			boolean autoCloseRegistry,
			BootstrapServiceRegistry bootstrapServiceRegistry,
			List<StandardServiceInitiator<?>> serviceInitiators,
			List<ProvidedService<?>> providedServices,
			Map<String,Object> configurationValues) {

		ServiceRegistryTestingImpl instance = new ServiceRegistryTestingImpl( autoCloseRegistry, bootstrapServiceRegistry, configurationValues );
		instance.initialize();
		instance.applyServiceRegistrations( serviceInitiators, providedServices );

		return instance;
	}
}
