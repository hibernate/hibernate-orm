/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import java.io.Serializable;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * Provides access to a {@link TransactionSynchronizationRegistry} for use by {@link TransactionSynchronizationRegistry}-based
 * {@link JtaSynchronizationStrategy} implementations.
 *
 * @author Steve Ebersole
 */
public interface SynchronizationRegistryAccess extends Serializable {
	/**
	 * Obtain the synchronization registry
	 *
	 * @return the synchronization registry
	 */
	TransactionSynchronizationRegistry getSynchronizationRegistry();
}
