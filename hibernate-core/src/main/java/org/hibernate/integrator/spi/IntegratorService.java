/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	Iterable<Integrator> getIntegrators();
}
