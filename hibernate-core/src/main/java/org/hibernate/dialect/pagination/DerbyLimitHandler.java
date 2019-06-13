/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.util.regex.Pattern;

/**
 * A {@link LimitHandler} for Apache Derby.
 *
 * <p/>
 * From Derby 10.5 Docs:
 * <pre>
 * Query
 * [ORDER BY clause]
 * [OFFSET n ROWS]
 * [FETCH {FIRST|NEXT} m ROWS ONLY]
 * [FOR {UPDATE|READ ONLY|FETCH ONLY}]
 * [WITH {RR|RS|CS|UR}]
 * </pre>
 *
 * @author Gavin King
 */
public class DerbyLimitHandler extends OffsetFetchLimitHandler {

	public DerbyLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	private static final Pattern FOR_UPDATE_WITH_LOCK_PATTERN =
			Pattern.compile("\\s+for\\s+(update|read|fetch)\\b|\\s+with\\s+(rr|rs|cs|ur)\\b|\\s*(;|$)");

	@Override
	protected Pattern getForUpdatePattern() {
		return FOR_UPDATE_WITH_LOCK_PATTERN;
	}
}
