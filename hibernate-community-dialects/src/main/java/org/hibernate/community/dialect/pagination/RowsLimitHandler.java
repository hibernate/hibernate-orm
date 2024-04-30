/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.pagination;

import java.util.regex.Pattern;

import org.hibernate.dialect.pagination.AbstractSimpleLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

/**
 * A {@link LimitHandler} that works in Interbase and Firebird,
 * using the syntax {@code ROWS n} and {@code ROWS m TO n}.
 * Note that this syntax does not allow specification of an
 * offset without a limit.
 *
 * @author Gavin King
 */
public class RowsLimitHandler extends AbstractSimpleLimitHandler {

	public static final RowsLimitHandler INSTANCE = new RowsLimitHandler();

	@Override
	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " rows ? to ?" : " rows ?";
	}

	@Override
	protected String offsetOnlyClause() {
		return " rows ? to " + Integer.MAX_VALUE;
	}

	@Override
	public final boolean useMaxForLimit() {
		return true;
	}

	@Override
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult + 1;
	}

	private static final Pattern FOR_UPDATE_PATTERN =
			compile("\\s+for\\s+update\\b|\\s+with\\s+lock\\b|\\s*;?\\s*$", CASE_INSENSITIVE);

	@Override
	protected Pattern getForUpdatePattern() {
		return FOR_UPDATE_PATTERN;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}
}
