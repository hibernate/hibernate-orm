/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

/**
 * A {@link LimitHandler} that works in Interbase, Firebird,
 * and TimesTen using the syntax {@code ROWS n} and
 * {@code ROWS m TO n}.
 *
 * @author Gavin King
 */
public class RowsLimitHandler extends AbstractLimitHandler {

	public static final RowsLimitHandler INSTANCE = new RowsLimitHandler();

	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( !hasMaxRows( selection ) ) {
			return sql;
		}
		String rows = hasFirstRow( selection )
				? " rows ? to ?"
				: " rows ?";
		return atStart()
				//for TimesTen
				? insertAfterSelect( rows, sql )
				//for others
				: insertBeforeForUpdate( sql, rows );
	}

	@Override
	public final boolean bindLimitParametersFirst() {
		return atStart();
	}

	@Override
	public final boolean useMaxForLimit() {
		return true;
	}

	@Override
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult + 1;
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return true;
	}

	protected boolean atStart() {
		return false;
	}

	private static final Pattern FOR_UPDATE_PATTERN =
			compile("\\s+for\\s+update\\b|\\s+with\\s+lock\\b|\\s*(;|$)", CASE_INSENSITIVE);

	@Override
	protected Pattern getForUpdatePattern() {
		return FOR_UPDATE_PATTERN;
	}
}
