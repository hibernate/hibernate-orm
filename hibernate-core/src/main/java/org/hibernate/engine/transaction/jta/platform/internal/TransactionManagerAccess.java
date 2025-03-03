/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import java.io.Serializable;
import jakarta.transaction.TransactionManager;

/**
 * Provides access to a {@link TransactionManager} for use by {@link TransactionManager}-based
 * {@link JtaSynchronizationStrategy} implementations.
 *
 * @author Steve Ebersole
 */
public interface TransactionManagerAccess extends Serializable {
	/**
	 * Obtain the transaction manager
	 *
	 * @return The transaction manager.
	 */
	TransactionManager getTransactionManager();
}
