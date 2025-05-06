/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import org.hibernate.query.spi.Limit;

/**
 * A {@link LimitHandler} for DB2. Uses {@code FETCH FIRST n ROWS ONLY},
 * together with {@code ROWNUMBER()} when there is an offset. (DB2 does
 * not support the ANSI syntax {@code OFFSET n ROWS}.)
 */
public class LegacyDB2LimitHandler extends AbstractLimitHandler {

	public static final LegacyDB2LimitHandler INSTANCE = new LegacyDB2LimitHandler();

	@Override
	public String processSql(String sql, Limit limit) {
		if ( hasFirstRow( limit ) ) {
			//nest the main query in an outer select
			return "select * from (select row_.*,rownumber() over(order by order of row_) as rownumber_ from ("
					+ sql + fetchFirstRows( limit )
					+ ") as row_) as query_ where rownumber_>"
					+ limit.getFirstRow()
					+ " order by rownumber_";
		}
		else {
			//on DB2, offset/fetch comes after all the
			//various "for update"ish clauses
			return insertAtEnd( fetchFirstRows( limit ), sql );
		}
	}

	private String fetchFirstRows(Limit limit) {
		return " fetch first " + getMaxOrLimit( limit ) + " rows only";
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
