/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.service.Service;

/**
 * @author Steve Ebersole
 */
public interface ServiceRegistryScope {
	/**
	 * Generalized support for running exception-safe code using a ServiceRegistry to
	 * ensure proper shutdown
	 */
	static void using(Supplier<StandardServiceRegistry> ssrProducer, Consumer<ServiceRegistryScope> action) {
		try (final StandardServiceRegistry ssr = ssrProducer.get()) {
			action.accept( () -> ssr );
		}
	}

	StandardServiceRegistry getRegistry();

	default <S extends Service> void withService(Class<S> role, Consumer<S> action) {
		assert role != null;

		final S service = getRegistry().getService( role );

		if ( service == null ) {
			throw new IllegalArgumentException( "Could not locate requested service - " + role.getName() );
		}

		action.accept( service );
	}

	default <R, S extends Service> R fromService(Class<S> role, Function<S,R> action) {
		assert role != null;

		final S service = getRegistry().getService( role );

		if ( service == null ) {
			throw new IllegalArgumentException( "Could not locate requested service - " + role.getName() );
		}

		return action.apply( service );
	}

	default HibernatePersistenceConfiguration createPersistenceConfiguration(String persistenceUnitName) {
		final HibernatePersistenceConfiguration configuration = new HibernatePersistenceConfiguration( persistenceUnitName );
		final StandardServiceRegistry registry = getRegistry();

		final ConfigurationService configurationService = registry.requireService( ConfigurationService.class );
		configuration.properties( configurationService.getSettings() );

		return configuration;
	}

}
