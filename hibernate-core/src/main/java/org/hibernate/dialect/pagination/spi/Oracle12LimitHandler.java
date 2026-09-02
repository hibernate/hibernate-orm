/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.SPI;


/**
 * A {@link LimitHandler} for Oracle 12c and later.
 *
 * @author Gavin King
 * @since 8.0
 */
@SPI
public final class Oracle12LimitHandler extends AbstractLimitHandler {
	public static final Oracle12LimitHandler INSTANCE = new Oracle12LimitHandler();

	Oracle12LimitHandler() {
	}

	@Override
	public PaginationResult processSql(PaginationRequest request) {
		if ( request.isEmpty() ) {
			return PaginationResult.unchanged( request.sql() );
		}

		final int forUpdateIndex = getForUpdateIndex( request.sql() );
		return forUpdateIndex < 0
				? offsetFetch( request )
				: rowNumber( request, forUpdateIndex );
	}

	private PaginationResult offsetFetch(PaginationRequest request) {
		final StringBuilder clause = new StringBuilder();
		int position = request.jdbcParameterCount() + 1;
		if ( request.hasFirstRow() ) {
			clause.append( " offset " ).append( request.parameterMarker( position++ ) ).append( " rows" );
		}
		if ( request.hasMaxRows() ) {
			clause.append( request.hasFirstRow() ? " fetch next " : " fetch first " )
					.append( request.parameterMarker( position ) )
					.append( " rows only" );
		}
		return result( request, insertAtEnd( clause.toString(), request.sql() ) );
	}

	private PaginationResult rowNumber(PaginationRequest request, int forUpdateIndex) {
		final String sql = request.sql();
		final String forUpdateClause = sql.substring( forUpdateIndex );
		final String unlockedSql = sql.substring( 0, forUpdateIndex - 1 );
		final int firstPosition = request.jdbcParameterCount() + 1;
		final String paginated;
		if ( request.hasFirstRow() && request.hasMaxRows() ) {
			paginated = "select * from (select row_.*,rownum rownum_ from ("
					+ unlockedSql + ") row_ where rownum<=" + request.parameterMarker( firstPosition )
					+ ") where rownum_>" + request.parameterMarker( firstPosition + 1 );
		}
		else if ( request.hasFirstRow() ) {
			paginated = "select * from (" + unlockedSql + ") row_ where rownum>"
					+ request.parameterMarker( firstPosition );
		}
		else {
			paginated = "select * from (" + unlockedSql + ") where rownum<="
					+ request.parameterMarker( firstPosition );
		}

		final List<Integer> values = new ArrayList<>( 2 );
		if ( request.hasMaxRows() ) {
			values.add( maxOrLastRow( request ) );
		}
		if ( request.hasFirstRow() ) {
			values.add( getFirstRow( request ) );
		}
		return new PaginationResult(
				paginated + " " + forUpdateClause,
				new PaginationJdbcInstructions( List.of(), values, null, 0 )
		);
	}

	private int maxOrLastRow(PaginationRequest request) {
		final int value = request.maxRows() + getFirstRow( request );
		return value < 0 ? Integer.MAX_VALUE : value;
	}

	private int getForUpdateIndex(String sql) {
		final int index = sql.toLowerCase( Locale.ROOT ).lastIndexOf( "for update" );
		final int lastQuote = sql.lastIndexOf( '\'' );
		return index > -1 && ( lastQuote == -1 || lastQuote < index ) ? index : -1;
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}
}
