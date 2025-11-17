/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jdbc.spi;

import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Models access to the resource transaction of the underlying JDBC resource.
 *
 * @author Steve Ebersole
 */
public interface JdbcResourceTransaction {
	/**
	 * Begin the resource transaction
	 */
	void begin();

	/**
	 * Commit the resource transaction
	 */
	void commit();

	/**
	 * Rollback the resource transaction
	 */
	void rollback();

	TransactionStatus getStatus();
}
