/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.sql.exec.internal.JdbcMutationExecutorImpl;

/**
 * @author Steve Ebersole
 */
public interface JdbcMutationExecutor {
	/**
	 * Singleton access (calling LogicalConnection#afterStatement afterwards)
	 */
	public static final JdbcMutationExecutor WITH_AFTER_STATEMENT_CALL = new JdbcMutationExecutorImpl( true );

	/**
	 * Singleton access (not calling LogicalConnection#afterStatement afterwards)
	 */
	public static final JdbcMutationExecutor NO_AFTER_STATEMENT_CALL = new JdbcMutationExecutorImpl( false );

	int execute(
			JdbcMutation jdbcMutation,
			ExecutionContext executionContext,
			PreparedStatementCreator statementCreator);
}
