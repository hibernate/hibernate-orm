/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;

import java.sql.Connection;

/**
 * An action to be performed after a {@linkplain PrimaryOperation}.
 */
@Incubating
@FunctionalInterface
public interface PostAction extends SecondaryAction {
	/**
	 * Perform the action.
	 * <p>
	 * Generally the action should use the passed {@code jdbcStatementAccess} to interact with the
	 * database, although the {@code jdbcConnection} can be used to create specialized statements,
	 * access the {@linkplain java.sql.DatabaseMetaData database metadata}, etc.
	 *
	 * @param jdbcStatementAccess Access to a JDBC Statement object which may be used to perform the action.
	 * @param jdbcConnection The JDBC Connection.
	 * @param executionContext Access to contextual information useful while executing.
	 * @param loadedValuesCollector Access to the collector of values loaded as part of the primary operation.  This is useful for post-actions that need to know what was loaded in order to perform their work.
	 */
	void performPostAction(StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext, @Nullable LoadedValuesCollector loadedValuesCollector);

	/**
	 * Should this post-action always be run even if the primary operation fails?
	 */
	default boolean shouldRunAfterFail() {
		return false;
	}
}
