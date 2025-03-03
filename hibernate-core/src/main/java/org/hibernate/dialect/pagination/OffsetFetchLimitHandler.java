/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import org.hibernate.query.spi.Limit;

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
	public String processSql(String sql, Limit limit) {

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
				offsetFetch.append( "?" );
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
				offsetFetch.append( "?" );
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
}
