/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.Remove;
import org.hibernate.service.Service;

/**
 * @author Steve Ebersole
 *
 * @apiNote This contract will be removed in 9.0.
 */
@Remove
public interface SessionFactoryServiceRegistryBuilder {
	@Nonnull
	SessionFactoryServiceRegistryBuilder addInitiator(@Nonnull SessionFactoryServiceInitiator<?> initiator);

	@Nonnull
	<R extends Service> SessionFactoryServiceRegistryBuilder addService(@Nonnull Class<R> serviceRole, R service);
}
