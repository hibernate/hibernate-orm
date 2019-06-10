/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * A {@link LimitHandler} for DB2. Uses {@code FETCH FIRST n ROWS ONLY},
 * together with {@code ROWNUMBER()} when there is an offset. (DB2 does
 * not support the ANSI syntax {@code OFFSET n ROWS}.)
 */
public class DB2LimitHandler extends AbstractLimitHandler {

	public static final DB2LimitHandler INSTANCE = new DB2LimitHandler();

	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( hasFirstRow( selection ) ) {
			//nest the main query in an outer select
			return "select * from ( select inner2_.*, rownumber() over(order by order of inner2_) as rownumber_ from ( "
					+ sql + fetchFirstRows( selection )
					+ " ) as inner2_ ) as inner1_ where rownumber_ > "
					+ selection.getFirstRow()
					+ " order by rownumber_";
		}
		else {
			return insertAtEnd( fetchFirstRows(selection), sql );
		}
	}

	private String fetchFirstRows(RowSelection selection) {
		return " fetch first " + getMaxOrLimit( selection ) + " rows only";
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public boolean supportsVariableLimit() {
		return false;
	}
}
