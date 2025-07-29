/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * Superclass for simple {@link LimitHandler}s that don't
 * support specifying an offset without a limit.
 *
 * @author Gavin King
 */
public abstract class AbstractSimpleLimitHandler extends AbstractLimitHandler {

	protected abstract String limitClause(boolean hasFirstRow);

	protected String limitClause(boolean hasFirstRow, int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		return limitClause( hasFirstRow );
	}

	protected String offsetOnlyClause() {
		return null;
	}

	protected String offsetOnlyClause(int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		return offsetOnlyClause();
	}

	@Override
	public String processSql(String sql, Limit limit) {
		return processSql( sql, -1, null, limit );
	}

	@Override
	public String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, QueryOptions queryOptions) {
		return processSql( sql, jdbcParameterCount, parameterMarkerStrategy, queryOptions.getLimit() );
	}

	private String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, @Nullable Limit limit) {
		final boolean hasFirstRow = hasFirstRow( limit );
		if ( !hasMaxRows( limit ) ) {
			final String offsetOnlyClause =
					ParameterMarkerStrategyStandard.isStandardRenderer( parameterMarkerStrategy )
							? offsetOnlyClause() : offsetOnlyClause( jdbcParameterCount, parameterMarkerStrategy );
			if ( offsetOnlyClause != null && hasFirstRow ) {
				return insert( offsetOnlyClause, sql );
			}
			return sql;
		}
		final String limitClause = ParameterMarkerStrategyStandard.isStandardRenderer( parameterMarkerStrategy )
				? limitClause( hasFirstRow ) : limitClause( hasFirstRow, jdbcParameterCount, parameterMarkerStrategy );
		return insert( limitClause, sql );
	}

	protected String insert(String limitClause, String sql) {
		return insertBeforeForUpdate( limitClause, sql );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return super.supportsOffset();
	}
}
