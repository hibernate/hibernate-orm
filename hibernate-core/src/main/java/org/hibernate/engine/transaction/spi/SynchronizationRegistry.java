/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.spi;

import java.io.Serializable;
import jakarta.transaction.Synchronization;

/**
 * Manages a registry of {@link Synchronization}s.
 *
 * @author Steve Ebersole
 */
public interface SynchronizationRegistry extends Serializable {
	/**
	 * Register a user {@link Synchronization} callback for this transaction.
	 *
	 * @param synchronization The synchronization callback to register.
	 *
	 * @throws org.hibernate.HibernateException
	 */
	public void registerSynchronization(Synchronization synchronization);

	/**
	 * Delegate {@link Synchronization#beforeCompletion} calls to the {@linkplain #registerSynchronization registered}
	 * {@link Synchronization}s
	 */
	void notifySynchronizationsBeforeTransactionCompletion();

	/**
	 * Delegate {@link Synchronization#afterCompletion} calls to {@linkplain #registerSynchronization registered}
	 * {@link Synchronization}s
	 *
	 * @param status The transaction status (if known) per {@link jakarta.transaction.Status}
	 */
	void notifySynchronizationsAfterTransactionCompletion(int status);
}
