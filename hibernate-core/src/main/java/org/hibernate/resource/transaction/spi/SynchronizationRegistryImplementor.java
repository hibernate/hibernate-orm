/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.spi;

/**
 * SPI contract for SynchronizationRegistry implementors.
 *
 * @author Steve Ebersole
 */
public interface SynchronizationRegistryImplementor extends SynchronizationRegistry {
	/**
	 * Delegates the {@link javax.transaction.Synchronization#beforeCompletion} call to each registered Synchronization
	 */
	void notifySynchronizationsBeforeTransactionCompletion();

	/**
	 * Delegates the {@link javax.transaction.Synchronization#afterCompletion} call to each registered Synchronization.  Will also
	 * clear the registered Synchronizations after all have been notified.
	 *
	 * @param status The transaction status, per {@link javax.transaction.Status} constants
	 */
	void notifySynchronizationsAfterTransactionCompletion(int status);

	/**
	 * Clears all synchronizations from this registry.  Note that synchronizations are automatically cleared during
	 * after-completion handling; see {@link #notifySynchronizationsAfterTransactionCompletion}
	 */
	void clearSynchronizations();
}
