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
package org.hibernate.integrator.internal;

import java.util.LinkedHashSet;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.secure.spi.JaccIntegrator;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class IntegratorServiceImpl implements IntegratorService {
	private static final Logger LOG = Logger.getLogger( IntegratorServiceImpl.class.getName() );

	private final LinkedHashSet<Integrator> integrators = new LinkedHashSet<Integrator>();

	public IntegratorServiceImpl(LinkedHashSet<Integrator> providedIntegrators, ClassLoaderService classLoaderService) {
		// register standard integrators.  Envers and JPA, for example, need to be handled by discovery because in
		// separate project/jars.
		addIntegrator( new BeanValidationIntegrator() );
		addIntegrator( new JaccIntegrator() );
		addIntegrator( new CollectionCacheInvalidator() );

		// register provided integrators
		for ( Integrator integrator : providedIntegrators ) {
			addIntegrator( integrator );
		}

		for ( Integrator integrator : classLoaderService.loadJavaServices( Integrator.class ) ) {
			addIntegrator( integrator );
		}
	}

	private void addIntegrator(Integrator integrator) {
		LOG.debugf( "Adding Integrator [%s].", integrator.getClass().getName() );
		integrators.add( integrator );
	}

	@Override
	public Iterable<Integrator> getIntegrators() {
		return integrators;
	}
}
