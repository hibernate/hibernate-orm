/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import org.hibernate.dialect.pagination.SQLServer2005LimitHandler.Keyword;

/**
 * A {@link LimitHandler} compatible with SQL Server 2012 which
 * introduced support for the ANSI SQL standard syntax
 * {@code OFFSET m ROWS FETCH NEXT n ROWS ONLY}, though this syntax
 * is considered part of the {@code ORDER BY} clause, and with the
 * wrinkle that both {@code ORDER BY} and the {@code OFFSET} clause
 * are required.
 *
 * @author Chris Cranford
 * @author Gavin King
 */
public class SQLServer2012LimitHandler extends OffsetFetchLimitHandler {

	// ORDER BY ...
	// [
	//   OFFSET m {ROW|ROWS}
	//   [FETCH {FIRST|NEXT} n {ROW|ROWS} ONLY]
	// ]

	public static final SQLServer2012LimitHandler INSTANCE = new SQLServer2012LimitHandler();

	public SQLServer2012LimitHandler() {
		super(true);
	}

	/**
	 * {@code OFFSET} and {@code FETCH} have to come right at the end
	 * of the {@code ORDER BY} clause, and {@code OFFSET} is required
	 * in order to have a {@code FETCH}:
	 * <pre>order by ... offset m rows [fetch next n rows only]</pre>
	 */
	@Override
	void begin(String sql, StringBuilder offsetFetch, boolean hasFirstRow, boolean hasMaxRows) {

		//see https://docs.microsoft.com/en-us/sql/t-sql/queries/select-order-by-clause-transact-sql?view=sql-server-2017

		if ( Keyword.ORDER_BY.rootOffset( sql ) <= 0 ) {
			//we need to add a whole 'order by' clause
			offsetFetch.append(" order by ");
			int from = Keyword.FROM.rootOffset( sql );
			if ( from > 0 ) {
				//if we can find the end of the select
				//clause, we will add a dummy column to
				//it below, so order by that column
				// Always need an order by clause: https://blog.jooq.org/2014/05/13/sql-server-trick-circumvent-missing-order-by-clause/
				offsetFetch.append("@@version");
			}
			else {
				//otherwise order by the first column
				offsetFetch.append("1");
			}
		}

		if ( !hasFirstRow ) {
			//the offset clause is required, but
			//the superclass doesn't add it
			offsetFetch.append(" offset 0 rows");
		}
	}
}
