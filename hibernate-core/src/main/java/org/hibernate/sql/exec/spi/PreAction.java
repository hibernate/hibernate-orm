/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;

import java.sql.Connection;

/**
 * An action to be performed before a {@linkplain PrimaryOperation}.
 */
@Incubating
@FunctionalInterface
public interface PreAction extends SecondaryAction {
	/**
	 * Perform the action.
	 * <p/>
	 * Generally the action should use the passed {@code jdbcStatementAccess} to interact with the
	 * database, although the {@code jdbcConnection} can be used to create specialized statements,
	 * access the {@linkplain java.sql.DatabaseMetaData database metadata}, etc.
	 *
	 * @param jdbcStatementAccess Access to a JDBC Statement object which may be used to perform the action.
	 * @param jdbcConnection The JDBC Connection.
	 * @param executionContext Access to contextual information useful while executing.
	 */
	void performPreAction(StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext);
}
