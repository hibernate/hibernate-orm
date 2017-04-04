/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.integrator.spi;

import org.hibernate.service.Service;

/**
 * @author Steve Ebersole
 */
public interface IntegratorService extends Service {
	/**
	 * Retrieve all integrators.
	 *
	 * @return All integrators.
	 */
	public Iterable<Integrator> getIntegrators();
}
