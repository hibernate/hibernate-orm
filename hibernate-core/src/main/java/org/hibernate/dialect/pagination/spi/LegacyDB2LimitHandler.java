/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import org.hibernate.SPI;


/**
 * A {@link LimitHandler} for DB2. Uses {@code FETCH FIRST n ROWS ONLY},
 * together with {@code ROWNUMBER()} when there is an offset. (DB2 does
 * not support the ANSI syntax {@code OFFSET n ROWS}.)
 *
 * @since 8.0
 */
@SPI
public final class LegacyDB2LimitHandler extends AbstractLimitHandler {

	public static final LegacyDB2LimitHandler INSTANCE = new LegacyDB2LimitHandler();

	private LegacyDB2LimitHandler() {
	}

	@Override
	public PaginationResult processSql(PaginationRequest request) {
		final String sql = request.sql();
		if ( request.isEmpty() ) {
			return PaginationResult.unchanged( sql );
		}
		if ( request.hasFirstRow() ) {
			//nest the main query in an outer select
			return result( request, "select * from (select row_.*,rownumber() over(order by order of row_) as rownumber_ from ("
					+ sql + fetchFirstRows( request )
					+ ") as row_) as query_ where rownumber_>"
					+ request.firstRow()
					+ " order by rownumber_" );
		}
		else {
			//on DB2, offset/fetch comes after all the
			//various "for update"ish clauses
			return result( request, insertAtEnd( fetchFirstRows( request ), sql ) );
		}
	}

	private String fetchFirstRows(PaginationRequest request) {
		return " fetch first " + getMaxOrLimit( request ) + " rows only";
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean useMaxForLimit() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return false;
	}

}
