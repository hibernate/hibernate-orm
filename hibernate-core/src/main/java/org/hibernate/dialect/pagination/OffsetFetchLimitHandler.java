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
 * A {@link LimitHandler} for databases which support the
 * ANSI SQL standard syntax {@code FETCH FIRST m ROWS ONLY}
 * and {@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}.
 *
 * @author Gavin King
 */
public class OffsetFetchLimitHandler extends AbstractLimitHandler {

	public static final OffsetFetchLimitHandler INSTANCE = new OffsetFetchLimitHandler(true);

	private final boolean variableLimit;

	public OffsetFetchLimitHandler(boolean variableLimit) {
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
		boolean hasFirstRow = hasFirstRow(limit);
		boolean hasMaxRows = hasMaxRows(limit);

		if ( !hasFirstRow && !hasMaxRows ) {
			return sql;
		}

		StringBuilder offsetFetch = new StringBuilder();

		begin(sql, offsetFetch, hasFirstRow, hasMaxRows);

		if ( hasFirstRow ) {
			offsetFetch.append( " offset " );
			if ( supportsVariableLimit() ) {
				if ( ParameterMarkerStrategyStandard.isStandardRenderer( parameterMarkerStrategy ) ) {
					offsetFetch.append( "?" );
				}
				else {
					offsetFetch.append( parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null ) );
				}
			}
			else {
				offsetFetch.append( limit.getFirstRow() );
			}
			if ( renderOffsetRowsKeyword() ) {
				offsetFetch.append( " rows" );
			}

		}
		if ( hasMaxRows ) {
			if ( hasFirstRow ) {
				offsetFetch.append( " fetch next " );
			}
			else {
				offsetFetch.append( " fetch first " );
			}
			if ( supportsVariableLimit() ) {
				if ( ParameterMarkerStrategyStandard.isStandardRenderer( parameterMarkerStrategy ) ) {
					offsetFetch.append( "?" );
				}
				else {
					offsetFetch.append(
							parameterMarkerStrategy.createMarker( jdbcParameterCount + (hasFirstRow ? 2 : 1), null ) );
				}
			}
			else {
				offsetFetch.append( getMaxOrLimit( limit ) );
			}
			offsetFetch.append( " rows only" );
		}

		return insert( offsetFetch.toString(), sql );
	}

	void begin(String sql, StringBuilder offsetFetch, boolean hasFirstRow, boolean hasMaxRows) {}

	String insert(String offsetFetch, String sql) {
		return insertBeforeForUpdate( offsetFetch, sql );
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
	public final boolean supportsVariableLimit() {
		return variableLimit;
	}

	protected boolean renderOffsetRowsKeyword() {
		return true;
	}

	@Override
	public boolean processSqlMutatesState() {
		return false;
	}
}
