/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.Internal;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/// Owns registries during bootstrap until cleanup or handoff to factory shutdown.
/// Only registries created for this bootstrap operation should be registered here.
///
/// @since 8.0
/// @author Steve Ebersole
@Internal
public final class BootstrapRegistryLifecycle implements AutoCloseable {
	private final ServiceRegistry bootstrapRegistry;
	private StandardServiceRegistry standardRegistry;
	private boolean released;

	public BootstrapRegistryLifecycle(ServiceRegistry bootstrapRegistry) {
		this.bootstrapRegistry = bootstrapRegistry;
	}

	public StandardServiceRegistry register(StandardServiceRegistry registry) {
		standardRegistry = registry;
		return registry;
	}

	/// Leaves registry destruction to the successfully constructed factory.
	public void transferOwnership() {
		released = true;
	}

	/// Releases owned registries without replacing an existing bootstrap failure.
	public void close(Throwable failure) {
		if ( !released ) {
			released = true;
			closeRegistry( bootstrapRegistry, closeRegistry( standardRegistry, failure ) );
		}
	}

	@Override
	public void close() {
		if ( !released ) {
			released = true;
			final var failure = closeRegistry( bootstrapRegistry, closeRegistry( standardRegistry, null ) );
			if ( failure instanceof RuntimeException exception ) {
				throw exception;
			}
			if ( failure instanceof Error error ) {
				throw error;
			}
		}
	}

	private static Throwable closeRegistry(ServiceRegistry registry, Throwable failure) {
		try {
			if ( registry != null
					&& (!(registry instanceof ServiceRegistryImplementor implementor) || implementor.isActive()) ) {
				registry.close();
			}
		}
		catch (RuntimeException | Error cleanupFailure) {
			if ( failure == null ) {
				return cleanupFailure;
			}
			if ( failure != cleanupFailure ) {
				failure.addSuppressed( cleanupFailure );
			}
		}
		return failure;
	}
}
