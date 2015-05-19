/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.spi;

import java.io.Serializable;
import javax.transaction.Synchronization;

/**
 * Manages a registry of {@link Synchronization Synchronizations}.
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
	 * Delegate {@link Synchronization#beforeCompletion} calls to the {@link #registerSynchronization registered}
	 * {@link Synchronization Synchronizations}
	 */
	void notifySynchronizationsBeforeTransactionCompletion();

	/**
	 * Delegate {@link Synchronization#afterCompletion} calls to {@link #registerSynchronization registered}
	 * {@link Synchronization Synchronizations}
	 *
	 * @param status The transaction status (if known) per {@link javax.transaction.Status}
	 */
	void notifySynchronizationsAfterTransactionCompletion(int status);
}
