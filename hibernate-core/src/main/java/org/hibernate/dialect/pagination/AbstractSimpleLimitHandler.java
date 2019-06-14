/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * Superclass for simple {@link LimitHandler}s that don't
 * support specifying an offset without a limit.
 *
 * @author Gavin King
 */
public abstract class AbstractSimpleLimitHandler extends AbstractLimitHandler {

	protected abstract String limitClause(boolean hasFirstRow);

	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( !hasMaxRows( selection) ) {
			return sql;
		}
		return insert( limitClause( hasFirstRow(selection) ), sql );
	}

	protected String insert(String limitClause, String sql) {
		return insertBeforeForUpdate( limitClause, sql );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return true;
	}
}
