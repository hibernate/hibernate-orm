/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.query.spi.SqlOmittingQueryOptions;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.BasicType;

/**
 * A {@link org.hibernate.sql.ast.tree.expression.JdbcParameter} which
 * binds the value of the first row from the {@code QueryOptions} limit
 * of the current execution.
 */
public class OffsetJdbcParameter extends AbstractJdbcParameter {

	public OffsetJdbcParameter(BasicType<Integer> type) {
		super( type );
	}

	@Override
	public void bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			JdbcParameterBindings jdbcParamBindings,
			ExecutionContext executionContext) throws SQLException {
		final var qo = executionContext.getQueryOptions();
		final var limit = qo instanceof SqlOmittingQueryOptions wrapped
				? wrapped.peekOriginalLimit()
				: qo.getLimit();
		final Integer firstRow = limit == null ? null : limit.getFirstRow();
		//noinspection unchecked
		getJdbcMapping().getJdbcValueBinder().bind(
				statement,
				firstRow == null ? 0 : firstRow,
				startPosition,
				executionContext.getSession()
		);
	}
}
