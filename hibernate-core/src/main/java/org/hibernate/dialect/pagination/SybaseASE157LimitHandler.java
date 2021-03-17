/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.engine.spi.RowSelection;

/**
 * This limit handler is very conservative and is only triggered in simple cases involving a select or select distinct.
 * <p>
 * Note that if the query already contains "top" just after the select or select distinct, we don't add anything to the
 * query. It might just be a column name but, in any case, we just don't add the top clause and default to the previous
 * behavior so it's not an issue.
 */
public class SybaseASE157LimitHandler extends AbstractLimitHandler {

	private static final Pattern SELECT_DISTINCT_PATTERN = Pattern.compile( "^(\\s*select\\s+distinct\\s+).*",
			Pattern.CASE_INSENSITIVE );
	private static final Pattern SELECT_PATTERN = Pattern.compile( "^(\\s*select\\s+).*", Pattern.CASE_INSENSITIVE );
	private static final Pattern TOP_PATTERN = Pattern.compile( "^\\s*top\\s+.*", Pattern.CASE_INSENSITIVE );

	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( selection.getMaxRows() == null ) {
			return sql;
		}

		int top = getMaxOrLimit( selection );
		if ( top == Integer.MAX_VALUE ) {
			return sql;
		}

		Matcher selectDistinctMatcher = SELECT_DISTINCT_PATTERN.matcher( sql );
		if ( selectDistinctMatcher.matches() ) {
			return insertTop( selectDistinctMatcher, sql, top );
		}

		Matcher selectMatcher = SELECT_PATTERN.matcher( sql );
		if ( selectMatcher.matches() ) {
			return insertTop( selectMatcher, sql, top );
		}

		return sql;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsLimitOffset() {
		return false;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public boolean supportsVariableLimit() {
		return false;
	}

	private static String insertTop(Matcher matcher, String sql, int top) {
		int end = matcher.end( 1 );

		if ( TOP_PATTERN.matcher( sql.substring( end ) ).matches() ) {
			return sql;
		}

		StringBuilder sb = new StringBuilder( sql );
		sb.insert( end, "top " + top + " " );
		return sb.toString();
	}
}
