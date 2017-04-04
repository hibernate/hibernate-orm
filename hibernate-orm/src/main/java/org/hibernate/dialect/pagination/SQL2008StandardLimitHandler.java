/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * LIMIT clause handler compatible with ISO and ANSI SQL:2008 standard.
 * 
 * @author zhouyanming (zhouyanming@gmail.com)
 */
public class SQL2008StandardLimitHandler extends AbstractLimitHandler {

	public static final SQL2008StandardLimitHandler INSTANCE = new SQL2008StandardLimitHandler();

	/**
	 * Constructs a SQL2008StandardLimitHandler
	 */
	private SQL2008StandardLimitHandler() {
		// NOP
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		if (LimitHelper.useLimit( this, selection )) {
			return sql + (LimitHelper.hasFirstRow( selection ) ?
					" offset ? rows fetch next ? rows only" : " fetch first ? rows only");
		}
		else {
			// or return unaltered SQL
			return sql;
		}
	}

}
