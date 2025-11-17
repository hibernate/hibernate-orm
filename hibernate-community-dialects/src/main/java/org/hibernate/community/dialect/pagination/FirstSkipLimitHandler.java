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
 * A {@link LimitHandler} for Firebird 2.5 and older which supports the syntax
 * {@code FIRST n SKIP m}.
 */
public class FirstSkipLimitHandler extends AbstractLimitHandler {

	public static final FirstSkipLimitHandler INSTANCE = new FirstSkipLimitHandler();

	@Override
	public String processSql(String sql, Limit limit) {
		return processSql( sql, -1, null, limit );
	}

	@Override
	public String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, QueryOptions queryOptions) {
		return processSql( sql, jdbcParameterCount, parameterMarkerStrategy, queryOptions.getLimit() );
	}

	private String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, Limit limit) {
		boolean hasFirstRow = hasFirstRow( limit );
		boolean hasMaxRows = hasMaxRows( limit );

		if ( !hasFirstRow && !hasMaxRows ) {
			return sql;
		}

		StringBuilder skipFirst = new StringBuilder();

		if ( ParameterMarkerStrategyStandard.isStandardRenderer( parameterMarkerStrategy ) ) {
			if ( hasMaxRows ) {
				skipFirst.append( " first ?" );
			}
			if ( hasFirstRow ) {
				skipFirst.append( " skip ?" );
			}
		}
		else {
			String marker = parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null );
			if ( hasMaxRows ) {
				skipFirst.append( " first " );
				skipFirst.append( marker );
				marker = parameterMarkerStrategy.createMarker( jdbcParameterCount + 2, null );
			}
			if ( hasFirstRow ) {
				skipFirst.append( " skip " );
				skipFirst.append( marker );
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
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public final boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	public boolean processSqlMutatesState() {
		return false;
	}

	@Override
	public int getParameterPositionStart(Limit limit) {
		return hasMaxRows( limit )
				? hasFirstRow( limit ) ? 3 : 2
				: hasFirstRow( limit ) ? 2 : 1;
	}

}
