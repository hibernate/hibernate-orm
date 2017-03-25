/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.util.Locale;

import org.hibernate.engine.spi.RowSelection;


/**
 * @author Brett Meyer
 */
public class TopLimitHandler extends AbstractLimitHandler {
	
	private final boolean supportsVariableLimit;
	
	private final boolean bindLimitParametersFirst;

	public TopLimitHandler(boolean supportsVariableLimit, boolean bindLimitParametersFirst) {
		this.supportsVariableLimit = supportsVariableLimit;
		this.bindLimitParametersFirst = bindLimitParametersFirst;
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
	public boolean supportsLimitOffset() {
		return supportsVariableLimit;
	}
	
	public boolean bindLimitParametersFirst() {
		return bindLimitParametersFirst;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		if (LimitHelper.hasFirstRow( selection )) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}

		final int selectIndex = sql.toLowerCase(Locale.ROOT).indexOf( "select" );
		final int selectDistinctIndex = sql.toLowerCase(Locale.ROOT).indexOf( "select distinct" );
		final int insertionPoint = selectIndex + (selectDistinctIndex == selectIndex ? 15 : 6);

		StringBuilder sb = new StringBuilder( sql.length() + 8 )
				.append( sql );
		
		if ( supportsVariableLimit ) {
			sb.insert( insertionPoint, " TOP ? " );
		}
		else {
			sb.insert( insertionPoint, " TOP " + getMaxOrLimit( selection ) + " " );
		}
		
		return sb.toString();
	}
}
