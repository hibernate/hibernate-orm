/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.DatabaseOperation;

import java.sql.Connection;

/**
 * An action to be performed before or after the primary action of a DatabaseOperation.
 *
 * @see DatabaseOperation#getPrimaryOperation()
 *
 * @author Steve Ebersole
 */
public interface JdbcAction {
	/**
	 * Perform the action.
	 * <p/>
	 * Generally the action should use the passed {@code jdbcStatement} to interact with the
	 * database, although the {@code jdbcConnection} can be used to create specialized statements,
	 * access the {@linkplain java.sql.DatabaseMetaData database metadata}, etc.
	 *
	 * @param jdbcStatementAccess Access to a JDBC Statement object which may be used to perform the action.
	 * @param jdbcConnection The JDBC Connection.
	 * @param executionContext Access to contextual information useful while executing.
	 */
	void perform(StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext);
}
