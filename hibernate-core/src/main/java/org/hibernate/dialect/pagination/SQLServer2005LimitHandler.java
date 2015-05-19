/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.engine.spi.RowSelection;
import org.hibernate.internal.util.StringHelper;

/**
 * LIMIT clause handler compatible with SQL Server 2005 and later.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class SQLServer2005LimitHandler extends AbstractLimitHandler {
	private static final String SELECT = "select";
	private static final String SELECT_WITH_SPACE = SELECT + ' ';
	private static final String FROM = "from";
	private static final String DISTINCT = "distinct";
	private static final String ORDER_BY = "order by";

	private static final Pattern ALIAS_PATTERN = Pattern.compile( "(?i)\\sas\\s(.)+$" );

	// Flag indicating whether TOP(?) expression has been added to the original query.
	private boolean topAdded;

	/**
	 * Constructs a SQLServer2005LimitHandler
	 */
	public SQLServer2005LimitHandler() {
		// NOP
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
		return true;
	}

	@Override
	public boolean supportsVariableLimit() {
		return true;
	}

	@Override
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		// Our dialect paginated results aren't zero based. The first row should get the number 1 and so on
		return zeroBasedFirstResult + 1;
	}

	/**
	 * Add a LIMIT clause to the given SQL SELECT (HHH-2655: ROW_NUMBER for Paging)
	 *
	 * The LIMIT SQL will look like:
	 *
	 * <pre>
	 * WITH query AS (
	 *   SELECT inner_query.*
	 *        , ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__
	 *     FROM ( original_query_with_top_if_order_by_present_and_all_aliased_columns ) inner_query
	 * )
	 * SELECT alias_list FROM query WHERE __hibernate_row_nr__ >= offset AND __hibernate_row_nr__ < offset + last
	 * </pre>
	 *
	 * When offset equals {@literal 0}, only <code>TOP(?)</code> expression is added to the original query.
	 *
	 * @return A new SQL statement with the LIMIT clause applied.
	 */
	@Override
	public String processSql(String sql, RowSelection selection) {
		final StringBuilder sb = new StringBuilder( sql );
		if ( sb.charAt( sb.length() - 1 ) == ';' ) {
			sb.setLength( sb.length() - 1 );
		}

		if ( LimitHelper.hasFirstRow( selection ) ) {
			final String selectClause = fillAliasInSelectClause( sb );

			final int orderByIndex = shallowIndexOfWord( sb, ORDER_BY, 0 );
			if ( orderByIndex > 0 ) {
				// ORDER BY requires using TOP.
				addTopExpression( sb );
			}

			encloseWithOuterQuery( sb );

			// Wrap the query within a with statement:
			sb.insert( 0, "WITH query AS (" ).append( ") SELECT " ).append( selectClause ).append( " FROM query " );
			sb.append( "WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?" );
		}
		else {
			addTopExpression( sb );
		}

		return sb.toString();
	}

	@Override
	public int bindLimitParametersAtStartOfQuery(RowSelection selection, PreparedStatement statement, int index) throws SQLException {
		if ( topAdded ) {
			// Binding TOP(?)
			statement.setInt( index, getMaxOrLimit( selection ) - 1 );
			return 1;
		}
		return 0;
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(RowSelection selection, PreparedStatement statement, int index) throws SQLException {
		return LimitHelper.hasFirstRow( selection ) ? super.bindLimitParametersAtEndOfQuery( selection, statement, index ) : 0;
	}

	/**
	 * Adds missing aliases in provided SELECT clause and returns coma-separated list of them.
	 * If query takes advantage of expressions like {@literal *} or {@literal {table}.*} inside SELECT clause,
	 * method returns {@literal *}.
	 *
	 * @param sb SQL query.
	 *
	 * @return List of aliases separated with comas or {@literal *}.
	 */
	protected String fillAliasInSelectClause(StringBuilder sb) {
		final List<String> aliases = new LinkedList<String>();
		final int startPos = shallowIndexOf( sb, SELECT_WITH_SPACE, 0 );
		int endPos = shallowIndexOfWord( sb, FROM, startPos );
		int nextComa = startPos;
		int prevComa = startPos;
		int unique = 0;
		boolean selectsMultipleColumns = false;

		while ( nextComa != -1 ) {
			prevComa = nextComa;
			nextComa = shallowIndexOf( sb, ",", nextComa );
			if ( nextComa > endPos ) {
				break;
			}
			if ( nextComa != -1 ) {
				final String expression = sb.substring( prevComa, nextComa );
				if ( selectsMultipleColumns( expression ) ) {
					selectsMultipleColumns = true;
				}
				else {
					String alias = getAlias( expression );
					if ( alias == null ) {
						// Inserting alias. It is unlikely that we would have to add alias, but just in case.
						alias = StringHelper.generateAlias( "page", unique );
						sb.insert( nextComa, " as " + alias );
						final int aliasExprLength = ( " as " + alias ).length();
						++unique;
						nextComa += aliasExprLength;
						endPos += aliasExprLength;
					}
					aliases.add( alias );
				}
				++nextComa;
			}
		}
		// Processing last column.
		// Refreshing end position, because we might have inserted new alias.
		endPos = shallowIndexOfWord( sb, FROM, startPos );
		final String expression = sb.substring( prevComa, endPos );
		if ( selectsMultipleColumns( expression ) ) {
			selectsMultipleColumns = true;
		}
		else {
			String alias = getAlias( expression );
			if ( alias == null ) {
				// Inserting alias. It is unlikely that we would have to add alias, but just in case.
				alias = StringHelper.generateAlias( "page", unique );
				sb.insert( endPos - 1, " as " + alias );
			}
			aliases.add( alias );
		}

		// In case of '*' or '{table}.*' expressions adding an alias breaks SQL syntax, returning '*'.
		return selectsMultipleColumns ? "*" : StringHelper.join( ", ", aliases.iterator() );
	}

	/**
	 * @param expression Select expression.
	 *
	 * @return {@code true} when expression selects multiple columns, {@code false} otherwise.
	 */
	private boolean selectsMultipleColumns(String expression) {
		final String lastExpr = expression.trim().replaceFirst( "(?i)(.)*\\s", "" );
		return "*".equals( lastExpr ) || lastExpr.endsWith( ".*" );
	}

	/**
	 * Returns alias of provided single column selection or {@code null} if not found.
	 * Alias should be preceded with {@code AS} keyword.
	 *
	 * @param expression Single column select expression.
	 *
	 * @return Column alias.
	 */
	private String getAlias(String expression) {
		final Matcher matcher = ALIAS_PATTERN.matcher( expression );
		if ( matcher.find() ) {
			// Taking advantage of Java regular expressions greedy behavior while extracting the last AS keyword.
			// Note that AS keyword can appear in CAST operator, e.g. 'cast(tab1.col1 as varchar(255)) as col1'.
			return matcher.group( 0 ).replaceFirst( "(?i)(.)*\\sas\\s", "" ).trim();
		}
		return null;
	}

	/**
	 * Encloses original SQL statement with outer query that provides {@literal __hibernate_row_nr__} column.
	 *
	 * @param sql SQL query.
	 */
	protected void encloseWithOuterQuery(StringBuilder sql) {
		sql.insert( 0, "SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " );
		sql.append( " ) inner_query " );
	}

	/**
	 * Adds {@code TOP} expression. Parameter value is bind in
	 * {@link #bindLimitParametersAtStartOfQuery(RowSelection, PreparedStatement, int)} method.
	 *
	 * @param sql SQL query.
	 */
	protected void addTopExpression(StringBuilder sql) {
		final int distinctStartPos = shallowIndexOfWord( sql, DISTINCT, 0 );
		if ( distinctStartPos > 0 ) {
			// Place TOP after DISTINCT.
			sql.insert( distinctStartPos + DISTINCT.length(), " TOP(?)" );
		}
		else {
			final int selectStartPos = shallowIndexOf( sql, SELECT_WITH_SPACE, 0 );
			// Place TOP after SELECT.
			sql.insert( selectStartPos + SELECT.length(), " TOP(?)" );
		}
		topAdded = true;
	}

	/**
	 * Returns index of the first case-insensitive match of search term surrounded by spaces
	 * that is not enclosed in parentheses.
	 *
	 * @param sb String to search.
	 * @param search Search term.
	 * @param fromIndex The index from which to start the search.
	 *
	 * @return Position of the first match, or {@literal -1} if not found.
	 */
	private static int shallowIndexOfWord(final StringBuilder sb, final String search, int fromIndex) {
		final int index = shallowIndexOf( sb, ' ' + search + ' ', fromIndex );
		// In case of match adding one because of space placed in front of search term.
		return index != -1 ? ( index + 1 ) : -1;
	}

	/**
	 * Returns index of the first case-insensitive match of search term that is not enclosed in parentheses.
	 *
	 * @param sb String to search.
	 * @param search Search term.
	 * @param fromIndex The index from which to start the search.
	 *
	 * @return Position of the first match, or {@literal -1} if not found.
	 */
	private static int shallowIndexOf(StringBuilder sb, String search, int fromIndex) {
		// case-insensitive match
		final String lowercase = sb.toString().toLowerCase(Locale.ROOT);
		final int len = lowercase.length();
		final int searchlen = search.length();
		int pos = -1;
		int depth = 0;
		int cur = fromIndex;
		do {
			pos = lowercase.indexOf( search, cur );
			if ( pos != -1 ) {
				for ( int iter = cur; iter < pos; iter++ ) {
					final char c = sb.charAt( iter );
					if ( c == '(' ) {
						depth = depth + 1;
					}
					else if ( c == ')' ) {
						depth = depth - 1;
					}
				}
				cur = pos + searchlen;
			}
		} while ( cur < len && depth != 0 && pos != -1 );
		return depth == 0 ? pos : -1;
	}
}
