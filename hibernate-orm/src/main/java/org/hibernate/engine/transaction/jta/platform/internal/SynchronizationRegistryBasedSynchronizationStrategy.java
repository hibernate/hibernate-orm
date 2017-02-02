/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;

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
		synchronizationRegistryAccess.getSynchronizationRegistry().registerInterposedSynchronization(
				synchronization
		);
	}

	@Override
	public boolean canRegisterSynchronization() {
		final TransactionSynchronizationRegistry registry = synchronizationRegistryAccess.getSynchronizationRegistry();
		return JtaStatusHelper.isActive( registry.getTransactionStatus() ) && ! registry.getRollbackOnly();
	}
}
