/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.util.Locale;

import org.hibernate.engine.spi.RowSelection;

public class Informix10LimitHandler extends AbstractLimitHandler {

	public static final Informix10LimitHandler INSTANCE = new Informix10LimitHandler();

	private Informix10LimitHandler() {
		// Disallow instantiation
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		final boolean hasOffset = LimitHelper.hasFirstRow( selection );
		String sqlOffset = hasOffset ? " skip " + selection.getFirstRow() : "";
		String sqlLimit = " first " + getMaxOrLimit( selection );
		String sqlOffsetLimit = sqlOffset + sqlLimit;
		String result = new StringBuilder( sql.length() + 10 )
				.append( sql )
				.insert( sql.toLowerCase( Locale.ROOT ).indexOf( "select" ) + 6, sqlOffsetLimit ).toString();
		return result;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	public boolean useMaxForLimit() {
		return false;
	}

	@Override
	public boolean supportsLimitOffset() {
		return true;
	}

	@Override
	public boolean supportsVariableLimit() {
		return false;
	}
}
