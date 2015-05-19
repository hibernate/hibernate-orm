/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
