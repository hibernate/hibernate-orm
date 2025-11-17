/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.integrator.internal;

import java.util.LinkedHashSet;

import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;

import static org.hibernate.service.internal.ServiceLogger.SERVICE_LOGGER;

/**
 * @author Steve Ebersole
 */
public class IntegratorServiceImpl implements IntegratorService {

	private final LinkedHashSet<Integrator> integrators = new LinkedHashSet<>();

	public IntegratorServiceImpl(Iterable<Integrator> providedIntegrators, ClassLoaderService classLoaderService) {
		// Register standard integrators.
		// Envers, for example, needs to be handled by discovery because in separate project/jar.
		addIntegrator( integrators, new BeanValidationIntegrator() );
		addIntegrator( integrators, new CollectionCacheInvalidator() );

		// register provided integrators
		for ( var integrator : providedIntegrators ) {
			addIntegrator( integrators, integrator );
		}
		for ( var integrator : classLoaderService.loadJavaServices( Integrator.class ) ) {
			addIntegrator( integrators, integrator );
		}
	}

	private static void addIntegrator(LinkedHashSet<Integrator> integrators, Integrator integrator) {
		if ( SERVICE_LOGGER.isDebugEnabled() ) {
			SERVICE_LOGGER.addingIntegrator( integrator.getClass().getName() );
		}
		integrators.add( integrator );
	}

	@Override
	public Iterable<Integrator> getIntegrators() {
		return integrators;
	}
}
