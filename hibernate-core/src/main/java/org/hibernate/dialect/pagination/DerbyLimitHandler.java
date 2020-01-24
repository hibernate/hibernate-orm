/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.util.regex.Pattern;

/**
 * A {@link LimitHandler} for Apache Derby, which
 * fully supports the ANSI SQL standard syntax
 * {@code FETCH FIRST m ROWS ONLY} and
 * {@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}.
 *
 * @author Gavin King
 */
public class DerbyLimitHandler extends OffsetFetchLimitHandler {

	// [ORDER BY ...]
	// [OFFSET n {ROW|ROWS}]
	// [FETCH {FIRST|NEXT} m {ROW|ROWS} ONLY]
	// [FOR {UPDATE|READ ONLY|FETCH ONLY}]
	// [WITH {RR|RS|CS|UR}]

	public DerbyLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	private static final Pattern FOR_UPDATE_WITH_LOCK_PATTERN =
			Pattern.compile("\\s+for\\s+(update|read|fetch)\\b|\\s+with\\s+(rr|rs|cs|ur)\\b|\\s*(;|$)");

	/**
	 * The offset/fetch clauses must come before the
	 * {@code FOR UPDATE}ish and {@code WITH} clauses.
	 */
	@Override
	protected Pattern getForUpdatePattern() {
		//see https://db.apache.org/derby/docs/10.15/ref/rrefsqljoffsetfetch.html#rrefsqljoffsetfetch
		return FOR_UPDATE_WITH_LOCK_PATTERN;
	}
}
