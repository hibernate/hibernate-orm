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

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class IntegratorServiceImpl implements IntegratorService {

	private static final Logger LOG = Logger.getLogger( IntegratorServiceImpl.class );

	private final LinkedHashSet<Integrator> integrators = new LinkedHashSet<>();

	private IntegratorServiceImpl() {
	}

	public static IntegratorServiceImpl create(LinkedHashSet<Integrator> providedIntegrators, ClassLoaderService classLoaderService) {
		IntegratorServiceImpl instance = new IntegratorServiceImpl();

		// register standard integrators.  Envers and JPA, for example, need to be handled by discovery because in
		// separate project/jars.
		instance.addIntegrator( new BeanValidationIntegrator() );
		instance.addIntegrator( new CollectionCacheInvalidator() );

		// register provided integrators
		for ( Integrator integrator : providedIntegrators ) {
			instance.addIntegrator( integrator );
		}
		for ( Integrator integrator : classLoaderService.loadJavaServices( Integrator.class ) ) {
			instance.addIntegrator( integrator );
		}

		return instance;
	}

	private void addIntegrator(Integrator integrator) {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Adding Integrator [%s]", integrator.getClass().getName() );
		}
		integrators.add( integrator );
	}

	@Override
	public Iterable<Integrator> getIntegrators() {
		return integrators;
	}
}
