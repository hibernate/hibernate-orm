/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

/**
 * Allows services to be injected with the {@link org.hibernate.service.ServiceRegistry} during configuration phase.
 *
 * @author Steve Ebersole
 */
public interface ServiceRegistryAwareService {
	/**
	 * Callback to inject the registry.
	 *
	 * @param serviceRegistry The registry
	 */
	void injectServices(ServiceRegistryImplementor serviceRegistry);
}
