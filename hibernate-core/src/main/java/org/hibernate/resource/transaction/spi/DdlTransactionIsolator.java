/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.spi;

import java.sql.Connection;

import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * Provides access to a {@link Connection} that is isolated from any
 * "current transaction" with the designated purpose of performing DDL
 * commands.
 *
 * @author Steve Ebersole
 */
public interface DdlTransactionIsolator {
	JdbcContext getJdbcContext();

	/**
	 * Returns a {@link Connection} that is usable within the bounds of the
	 * {@link TransactionCoordinatorBuilder#buildDdlTransactionIsolator}
	 * and {@link #release} calls, with autocommit mode enabled. Further,
	 * this {@code Connection} will be isolated (transactionally) from any
	 * transaction in effect prior to the call to
	 * {@code buildDdlTransactionIsolator}.
	 *
	 * @return The Connection.
	 */
	Connection getIsolatedConnection();

	/**
	 * Returns a {@link Connection} that is usable within the bounds of the
	 * {@link TransactionCoordinatorBuilder#buildDdlTransactionIsolator}
	 * and {@link #release} calls, with the given autocommit mode. Further,
	 * this {@code Connection} will be isolated (transactionally) from any
	 * transaction in effect prior to the call to
	 * {@code buildDdlTransactionIsolator}.
	 *
	 * @return The Connection.
	 */
	Connection getIsolatedConnection(boolean autocommit);

	void release();
}
