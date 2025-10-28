/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * A {@link LimitHandler} for Informix which supports the syntax
 * {@code SKIP m FIRST n}.
 */
public class SkipFirstLimitHandler extends AbstractLimitHandler {

	public static final SkipFirstLimitHandler INSTANCE = new SkipFirstLimitHandler(true);

	private final boolean variableLimit;

	public SkipFirstLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	@Override
	public String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, QueryOptions queryOptions) {
		return processSql( sql, jdbcParameterCount, parameterMarkerStrategy, queryOptions.getLimit() );
	}

	@Override
	public String processSql(String sql, Limit limit) {
		return processSql( sql, -1, null, limit );
	}

	private String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, @Nullable Limit limit) {
		boolean hasFirstRow = hasFirstRow( limit );
		boolean hasMaxRows = hasMaxRows( limit );

		if ( !hasFirstRow && !hasMaxRows ) {
			return sql;
		}

		StringBuilder skipFirst = new StringBuilder();

		if ( supportsVariableLimit() ) {
			if ( ParameterMarkerStrategyStandard.isStandardRenderer( parameterMarkerStrategy ) ) {
				if ( hasFirstRow ) {
					skipFirst.append( " skip ?" );
				}
				if ( hasMaxRows ) {
					skipFirst.append( " first ?" );
				}
			}
			else {
				String marker = parameterMarkerStrategy.createMarker( 1, null );
				if ( hasMaxRows ) {
					skipFirst.append( " skip " );
					skipFirst.append( marker );
					marker = parameterMarkerStrategy.createMarker( 2, null );
				}
				if ( hasFirstRow ) {
					skipFirst.append( " first " );
					skipFirst.append( marker );
				}
			}
		}
		else {
			if ( hasFirstRow ) {
				skipFirst.append( " skip " )
						.append( limit.getFirstRow() );
			}
			if ( hasMaxRows ) {
				skipFirst.append( " first " )
						.append( getMaxOrLimit( limit ) );
			}
		}

		return insertAfterSelect( skipFirst.toString(), sql );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}

	@Override
	public final boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return variableLimit;
	}

	@Override
	public boolean processSqlMutatesState() {
		return false;
	}

	@Override
	public int getParameterPositionStart(Limit limit) {
		return supportsVariableLimit() && hasMaxRows( limit )
				? hasFirstRow( limit ) ? 3 : 2
				: supportsVariableLimit() && hasFirstRow( limit ) ? 2 : 1;
	}
}
