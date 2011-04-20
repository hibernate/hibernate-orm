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
import java.util.ServiceLoader;

import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.cfg.search.HibernateSearchIntegrator;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class IntegratorServiceImpl implements IntegratorService {
	private final ServiceRegistryImplementor serviceRegistry;
	private LinkedHashSet<Integrator> integrators = new LinkedHashSet<Integrator>();

	public IntegratorServiceImpl(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		// Standard integrators nameable from here.  Envers and JPA, for example, need to be handled by discovery
		// because in separate project/jars
		integrators.add( new BeanValidationIntegrator() );
		integrators.add( new HibernateSearchIntegrator() );
	}

	@Override
	public void addIntegrator(Integrator integrator) {
		integrators.add( integrator );
	}

	@Override
	public Iterable<Integrator> getIntegrators() {
		LinkedHashSet<Integrator> integrators = new LinkedHashSet<Integrator>();
		integrators.addAll( this.integrators );

		for ( Integrator integrator : ServiceLoader.load( Integrator.class ) ) {
			integrators.add( integrator );
		}

		return integrators;
	}
}
