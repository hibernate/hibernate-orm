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
package org.hibernate.integrator.spi;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * Additional, optional contract for Integrators that wish to contribute {@link org.hibernate.service.Service services}
 * to the Hibernate {@link org.hibernate.service.ServiceRegistry}.
 *
 * @author Steve Ebersole
 */
public interface ServiceContributingIntegrator extends Integrator {
	/**
	 * Allow the integrator to alter the builder of {@link org.hibernate.service.ServiceRegistry}, presumably to
	 * register services into it.
	 *
	 * @param serviceRegistryBuilder The build to prepare.
	 */
	public void prepareServices(StandardServiceRegistryBuilder serviceRegistryBuilder);
}
