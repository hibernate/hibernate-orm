/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.BasicType;

/**
 * A {@link org.hibernate.sql.ast.tree.expression.JdbcParameter} which
 * binds the value of the max rows from the {@code QueryOptions} limit
 * of the current execution.
 */
public class LimitJdbcParameter extends AbstractJdbcParameter {

	public LimitJdbcParameter(BasicType<Integer> type) {
		super( type );
	}

	@Override
	public void bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			JdbcParameterBindings jdbcParamBindings,
			ExecutionContext executionContext) throws SQLException {
		// Peek through any wrapper (e.g. SqlOmittingQueryOptions, the scroll
		// execution context) so we get the application-set limit even if
		// getLimit() has been suppressed for SQL rendering.
		final var limit = executionContext.getQueryOptions().peekOriginalLimit();
		final Integer maxRows = limit == null ? null : limit.getMaxRows();
		//noinspection unchecked
		getJdbcMapping().getJdbcValueBinder().bind(
				statement,
				maxRows == null ? Integer.MAX_VALUE : maxRows,
				startPosition,
				executionContext.getSession()
		);
	}
}
