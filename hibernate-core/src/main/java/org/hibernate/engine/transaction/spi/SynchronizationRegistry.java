/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
