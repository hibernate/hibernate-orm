/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

import static org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard.isStandardRenderer;

/**
 * Superclass for {@link LimitHandler}s that don't support
 * offsets at all.
 *
 * @author Gavin King
 */
public abstract class AbstractNoOffsetLimitHandler extends AbstractLimitHandler {

	private final boolean variableLimit;

	public AbstractNoOffsetLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	/**
	 * The SQL fragment to insert, with a ? placeholder
	 * for the actual numerical limit.
	 */
	protected abstract String limitClause();

	protected String limitClause(int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		return limitClause();
	}

	protected abstract String insert(String limitClause, String sql);

	@Override
	public String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, QueryOptions queryOptions) {
		return processSql( sql, jdbcParameterCount, parameterMarkerStrategy, queryOptions.getLimit() );
	}

	@Override
	public String processSql(String sql, Limit limit) {
		return processSql( sql, -1, null, limit );
	}

	private String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, @Nullable Limit limit) {
		if ( hasMaxRows( limit ) ) {
			final String limitClause;
			if ( supportsVariableLimit() ) {
				limitClause =
						isStandardRenderer( parameterMarkerStrategy )
								? limitClause()
								: limitClause( jdbcParameterCount, parameterMarkerStrategy );
			}
			else {
				limitClause =
						limitClause()
								.replace( "?",
										Integer.toString( getMaxOrLimit( limit ) ) );
			}
			return insert( limitClause, sql );
		}
		else {
			return sql;
		}
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsLimitOffset() {
		return false;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return variableLimit;
	}

	@Override
	public abstract boolean bindLimitParametersFirst();

}
