/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.internal.util.NullnessUtil;

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
	@NullnessUtil.Initializer
	void injectServices(@Nonnull ServiceRegistryImplementor serviceRegistry);
}
