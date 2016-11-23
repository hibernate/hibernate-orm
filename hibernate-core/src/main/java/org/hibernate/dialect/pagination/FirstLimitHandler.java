/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;


/**
 * @author Brett Meyer
 */
public class FirstLimitHandler extends LegacyFirstLimitHandler {

	public static final FirstLimitHandler INSTANCE = new FirstLimitHandler();

	private FirstLimitHandler() {
		// NOP
	}
	
	@Override
	public String processSql(String sql, RowSelection selection) {
		final boolean hasOffset = LimitHelper.hasFirstRow( selection );
		if ( hasOffset ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		return super.processSql( sql, selection );
	}
}
