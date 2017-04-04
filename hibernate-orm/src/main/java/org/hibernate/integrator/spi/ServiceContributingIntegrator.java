/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.integrator.spi;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * Additional, optional contract for Integrators that wish to contribute {@link org.hibernate.service.Service services}
 * to the Hibernate {@link org.hibernate.service.ServiceRegistry}.
 *
 * @author Steve Ebersole
 *
 * @deprecated A separate {@link org.hibernate.service.spi.ServiceContributor} should be used instead.
 */
@Deprecated
public interface ServiceContributingIntegrator extends Integrator {
	/**
	 * Allow the integrator to alter the builder of {@link org.hibernate.service.ServiceRegistry}, presumably to
	 * register services into it.
	 *
	 * @param serviceRegistryBuilder The build to prepare.
	 */
	public void prepareServices(StandardServiceRegistryBuilder serviceRegistryBuilder);
}
