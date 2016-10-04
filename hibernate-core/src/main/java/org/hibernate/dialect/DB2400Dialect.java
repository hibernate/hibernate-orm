/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.identity.DB2390IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;

/**
 * An SQL dialect for DB2/400.  This class provides support for DB2 Universal Database for iSeries,
 * also known as DB2/400.
 *
 * @author Peter DeGregorio (pdegregorio)
 */
public class DB2400Dialect extends DB2Dialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			if ( LimitHelper.hasFirstRow( selection ) ) {
				//nest the main query in an outer select
				return "select * from ( select inner2_.*, rownumber() over(order by order of inner2_) as rownumber_ from ( "
						+ sql + " fetch first " + getMaxOrLimit( selection ) + " rows only ) as inner2_ ) as inner1_ where rownumber_ > "
						+ selection.getFirstRow() + " order by rownumber_";
			}
			return sql + " fetch first " + getMaxOrLimit( selection ) + " rows only";
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
	};

	@Override
	public boolean supportsSequences() {
		return false;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimitOffset() {
		return false;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public boolean supportsVariableLimit() {
		return false;
	}

	@Override
	public String getLimitString(String sql, int offset, int limit) {
		if ( offset > 0 ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		if ( limit == 0 ) {
			return sql;
		}
		return sql + " fetch first " + limit + " rows only ";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public String getForUpdateString() {
		return " for update with rs";
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new DB2390IdentityColumnSupport();
	}
}
