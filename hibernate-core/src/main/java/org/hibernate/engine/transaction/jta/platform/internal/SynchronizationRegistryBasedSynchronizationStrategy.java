/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

import static org.hibernate.engine.transaction.internal.jta.JtaStatusHelper.isActive;

/**
 * Implementation of the {@link JtaSynchronizationStrategy} contract based on using a
 * {@link TransactionSynchronizationRegistry}
 *
 * @author Steve Ebersole
 */
public class SynchronizationRegistryBasedSynchronizationStrategy implements JtaSynchronizationStrategy {
	private final SynchronizationRegistryAccess synchronizationRegistryAccess;

	public SynchronizationRegistryBasedSynchronizationStrategy(SynchronizationRegistryAccess synchronizationRegistryAccess) {
		this.synchronizationRegistryAccess = synchronizationRegistryAccess;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		synchronizationRegistryAccess.getSynchronizationRegistry()
				.registerInterposedSynchronization( synchronization );
	}

	@Override
	public boolean canRegisterSynchronization() {
		final var registry = synchronizationRegistryAccess.getSynchronizationRegistry();
		return isActive( registry.getTransactionStatus() )
			&& !registry.getRollbackOnly();
	}
}
