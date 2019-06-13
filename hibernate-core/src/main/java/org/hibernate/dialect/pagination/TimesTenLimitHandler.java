/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

/**
 * A {@link LimitHandler} for TimesTen, which uses {@code ROWS n},
 * but at the start of the query instead of at the end.
 */
public class TimesTenLimitHandler extends RowsLimitHandler {

	public static final TimesTenLimitHandler INSTANCE = new TimesTenLimitHandler();

	@Override
	String insert(String sql, String rows) {
		return insertAfterSelect( rows, sql );
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return true;
	}
}
