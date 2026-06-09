/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;

import jakarta.annotation.Nullable;

/**
 * Additional integration contracts for a service registry.
 *
 * @author Steve Ebersole
 */
public interface ServiceRegistryImplementor extends ServiceRegistry {
	/**
	 * Locate the binding for the given role.  Should, generally speaking, look into parent registry if one.
	 *
	 * @param serviceRole The service role for which to locate a binding.
	 * @param <R> generic return type.
	 *
	 * @return The located binding; may be {@code null}
	 */
	<R extends Service> @Nullable ServiceBinding<R> locateServiceBinding(@Nonnull Class<R> serviceRole);

	@Override
	default void close() {
		destroy();
	}

	boolean isActive();

	/**
	 * Release resources
	 */
	void destroy();

	/**
	 * When a registry is created with a parent, the parent is notified of the child
	 * via this callback.
	 */
	void registerChild(@Nonnull ServiceRegistryImplementor child);

	/**
	 * When a registry is created with a parent, the parent is notified of the child
	 * via this callback.
	 */
	void deRegisterChild(@Nonnull ServiceRegistryImplementor child);

	<T extends Service> @Nullable T fromRegistryOrChildren(@Nonnull Class<T> serviceRole);
}
