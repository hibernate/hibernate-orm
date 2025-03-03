/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.spi;

import java.io.Serializable;
import jakarta.transaction.Synchronization;

import org.hibernate.resource.transaction.NullSynchronizationException;

/**
 * Manages a registry of (local) JTA {@link Synchronization} instances
 *
 * @author Steve Ebersole
 */
public interface SynchronizationRegistry extends Serializable {
	/**
	 * Register a {@link Synchronization} callback for this transaction.
	 *
	 * @param synchronization The synchronization callback to register.
	 *
	 * @throws NullSynchronizationException if the synchronization is {@code null}
	 */
	void registerSynchronization(Synchronization synchronization);
}
