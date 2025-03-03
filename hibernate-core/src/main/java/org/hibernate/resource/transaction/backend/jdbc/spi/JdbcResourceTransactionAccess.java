/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jdbc.spi;

/**
 * Provides access to {@link JdbcResourceTransaction} (JDBC transaction stand-in) for use in
 * building resource-local {@link org.hibernate.resource.transaction.spi.TransactionCoordinator}
 * instances.
 *
 * @author Steve Ebersole
 */
public interface JdbcResourceTransactionAccess {
	/**
	 * Provides access to the resource local transaction of this data store, which is used by the
	 * {@link org.hibernate.resource.transaction.spi.TransactionCoordinator} to manage transactions
	 * against the data store when not using JTA.
	 *
	 * @return The resource-local transaction
	 */
	JdbcResourceTransaction getResourceLocalTransaction();
}
