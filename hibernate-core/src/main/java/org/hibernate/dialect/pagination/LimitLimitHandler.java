/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

/**
 * Limit handler for MySQL and CUBRID which support the syntax
 * {@code LIMIT n} and {@code LIMIT m, n}. Note that this
 * syntax does not allow specification of an offset without
 * a limit.
 *
 * @author Esen Sagynov (kadishmal at gmail dot com)
 */
public class LimitLimitHandler extends AbstractSimpleLimitHandler {

	public static final LimitLimitHandler INSTANCE = new LimitLimitHandler();

	@Override
	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " limit ?,?" : " limit ?";
	}

	@Override
	protected String offsetOnlyClause() {
		return " limit ?," + Integer.MAX_VALUE;
	}

	private static final Pattern FOR_UPDATE_PATTERN =
			compile("\\s+for\\s+update\\b|\\s+lock\\s+in\\s+shared\\s+mode\\b|\\s*;?\\s*$", CASE_INSENSITIVE);

	@Override
	protected Pattern getForUpdatePattern() {
		return FOR_UPDATE_PATTERN;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}
}
