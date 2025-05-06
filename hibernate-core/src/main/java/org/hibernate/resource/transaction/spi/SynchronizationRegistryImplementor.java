/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.spi;

/**
 * SPI contract for {@link SynchronizationRegistry} implementors.
 *
 * @author Steve Ebersole
 */
public interface SynchronizationRegistryImplementor extends SynchronizationRegistry {
	/**
	 * Delegates the {@link jakarta.transaction.Synchronization#beforeCompletion} call to each registered Synchronization
	 */
	void notifySynchronizationsBeforeTransactionCompletion();

	/**
	 * Delegates the {@link jakarta.transaction.Synchronization#afterCompletion} call to each registered Synchronization.
	 * Will also clear the registered {@code Synchronization}s after all have been notified.
	 *
	 * @param status The transaction status, per {@link jakarta.transaction.Status} constants
	 */
	void notifySynchronizationsAfterTransactionCompletion(int status);

	/**
	 * Clears all synchronizations from this registry.  Note that synchronizations are automatically cleared during
	 * after-completion handling; see {@link #notifySynchronizationsAfterTransactionCompletion}
	 */
	void clearSynchronizations();
}
