/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * Limit handler for CUBRID
 *
 * @author Esen Sagynov (kadishmal at gmail dot com)
 */
public class CUBRIDLimitHandler extends AbstractLimitHandler {

	public static final CUBRIDLimitHandler INSTANCE = new CUBRIDLimitHandler();

	private CUBRIDLimitHandler() {
		// NOP
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( LimitHelper.useLimit( this, selection ) ) {
			// useLimitOffset: whether "offset" is set or not;
			// if set, use "LIMIT offset, row_count" syntax;
			// if not, use "LIMIT row_count"
			final boolean useLimitOffset = LimitHelper.hasFirstRow( selection );
			return sql + (useLimitOffset ? " limit ?, ?" : " limit ?");
		}
		else {
			// or return unaltered SQL
			return sql;
		}
	}
}
