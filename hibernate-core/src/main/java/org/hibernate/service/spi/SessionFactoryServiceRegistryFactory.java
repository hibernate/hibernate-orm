/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.metamodel.spi.SessionFactoryAccess;
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
	 * @param sessionFactoryAccess Access to the SessionFactory.  Generally this is useful
	 * for grabbing a reference for later use.  However, care should be taken when dereferencing
	 * the access object until after the SessionFactory has been fully initialized.
	 * @param sessionFactoryOptions The build options.
	 *
	 * @return The registry
	 */
	@Nonnull
	SessionFactoryServiceRegistry buildServiceRegistry(
			@Nonnull SessionFactoryAccess sessionFactoryAccess,
			@Nonnull SessionFactoryOptions sessionFactoryOptions);

}
