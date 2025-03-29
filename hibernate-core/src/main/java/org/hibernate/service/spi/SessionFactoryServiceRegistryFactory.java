/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.Service;

/**
 * Contract for builder of {@link SessionFactoryServiceRegistry} instances.
 * <p>
 * Is itself a service within the standard service registry.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryServiceRegistryFactory extends Service {
	/**
	 * Create the registry.
	 *
	 * @param sessionFactory The (still being built) session factory.  Generally this is useful
	 * for grabbing a reference for later use.  However, care should be taken when invoking on
	 * the session factory until after it has been fully initialized.
	 * @param sessionFactoryOptions The build options.
	 *
	 * @return The registry
	 */
	SessionFactoryServiceRegistry buildServiceRegistry(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions);

}
