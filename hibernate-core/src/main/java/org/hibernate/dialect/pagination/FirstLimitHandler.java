/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

import static java.lang.String.valueOf;

/**
 * A {@link LimitHandler} for older versions of Informix, Ingres,
 * and TimesTen, which supported the syntax {@code SELECT FIRST n}.
 *
 * @author Chris Cranford
 */
public class FirstLimitHandler extends AbstractLimitHandler {

	public static final FirstLimitHandler INSTANCE = new FirstLimitHandler(false);

	private boolean variableLimit;

	public FirstLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( !hasMaxRows( selection) ) {
			return sql;
		}
		String first = " first ?";
		if ( variableLimit ) {
			first = first.replace( "?", valueOf( getMaxOrLimit( selection ) ) );
		}
		return insertAfterSelect( sql, first );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsLimitOffset() {
		return false;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return variableLimit;
	}

	@Override
	public final boolean bindLimitParametersFirst() {
		return true;
	}
}
