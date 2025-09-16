/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

import java.sql.Connection;

/**
 * Primary operation which is a {@code SELECT} performed via JDBC.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JdbcSelect extends PrimaryOperation, CacheableJdbcOperation {
	JdbcValuesMappingProducer getJdbcValuesMappingProducer();
	JdbcLockStrategy getLockStrategy();
	boolean usesLimitParameters();
	JdbcParameter getLimitParameter();
	int getRowsToSkip();
	int getMaxRows();

	/**
	 * Access to a collector of values loaded to be applied during the
	 * processing of the selection's results.
	 * May be {@code null}.
	 */
	@Nullable
	LoadedValuesCollector getLoadedValuesCollector();

	/**
	 * Perform any pre-actions.
	 * <p/>
	 * Generally the pre-actions should use the passed {@code jdbcStatementAccess} to interact with the
	 * database, although the {@code jdbcConnection} can be used to create specialized statements,
	 * access the {@linkplain java.sql.DatabaseMetaData database metadata}, etc.
	 *
	 * @param jdbcStatementAccess Access to a JDBC Statement object which may be used to perform the action.
	 * @param jdbcConnection The JDBC Connection.
	 * @param executionContext Access to contextual information useful while executing.
	 */
	void performPreActions(StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext);	/**

	 * Perform any post-actions.
	 * <p/>
	 * Generally the post-actions should use the passed {@code jdbcStatementAccess} to interact with the
	 * database, although the {@code jdbcConnection} can be used to create specialized statements,
	 * access the {@linkplain java.sql.DatabaseMetaData database metadata}, etc.
	 *
	 * @param succeeded Whether the primary operation succeeded.
	 * @param jdbcStatementAccess Access to a JDBC Statement object which may be used to perform the action.
	 * @param jdbcConnection The JDBC Connection.
	 * @param executionContext Access to contextual information useful while executing.
	 */
	void performPostAction(
			boolean succeeded,
			StatementAccess jdbcStatementAccess,
			Connection jdbcConnection,
			ExecutionContext executionContext);

}
