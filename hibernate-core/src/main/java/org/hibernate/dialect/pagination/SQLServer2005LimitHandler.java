/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
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
 * @author Chris Cranford
 */
public class SQLServer2005LimitHandler extends AbstractLimitHandler {
	private static final String SELECT = "select";
	private static final String FROM = "from";
	private static final String DISTINCT = "distinct";
	private static final String ORDER_BY = "order by";
	private static final String SELECT_DISTINCT = SELECT + " " + DISTINCT;
	private static final String SELECT_DISTINCT_SPACE = SELECT_DISTINCT + " ";

	private static final String SELECT_SPACE = "select ";

	private static final Pattern SELECT_DISTINCT_PATTERN = buildShallowIndexPattern( SELECT_DISTINCT_SPACE, true );
	private static final Pattern SELECT_PATTERN = buildShallowIndexPattern( SELECT + "(.*)", true );
	private static final Pattern FROM_PATTERN = buildShallowIndexPattern( FROM, true );
	private static final Pattern DISTINCT_PATTERN = buildShallowIndexPattern( DISTINCT, true );
	private static final Pattern ORDER_BY_PATTERN = buildShallowIndexPattern( ORDER_BY, true );
	private static final Pattern COMMA_PATTERN = buildShallowIndexPattern( ",", false );
	private static final Pattern ALIAS_PATTERN =
			Pattern.compile( "(?![^\\[]*(\\]))\\S+\\s*(\\s(?i)as\\s)\\s*(\\S+)*\\s*$|(?![^\\[]*(\\]))\\s+(\\S+)$" );

	// CTE pattern support
	private static final String SPACE_NEWLINE_LINEFEED = "[\\s\\t\\n\\r]*";
	private static final Pattern WITH_CTE = Pattern.compile( "(^" + SPACE_NEWLINE_LINEFEED + "WITH" + SPACE_NEWLINE_LINEFEED +")", Pattern.CASE_INSENSITIVE );
	private static final Pattern WITH_EXPRESSION_NAME = Pattern.compile( "(^" + SPACE_NEWLINE_LINEFEED + "[a-zA-Z0-9]*" + SPACE_NEWLINE_LINEFEED +")", Pattern.CASE_INSENSITIVE  );
	private static final Pattern WITH_COLUMN_NAMES_START = Pattern.compile( "(^" + SPACE_NEWLINE_LINEFEED + "\\()", Pattern.CASE_INSENSITIVE );
	private static final Pattern WITH_COLUMN_NAMES_END = Pattern.compile( "(\\))", Pattern.CASE_INSENSITIVE );
	private static final Pattern WITH_AS = Pattern.compile( "(^" + SPACE_NEWLINE_LINEFEED + "AS" + SPACE_NEWLINE_LINEFEED +")", Pattern.CASE_INSENSITIVE );
	private static final Pattern WITH_COMMA = Pattern.compile( "(^" + SPACE_NEWLINE_LINEFEED + ",)", Pattern.CASE_INSENSITIVE );

	// Flag indicating whether TOP(?) expression has been added to the original query.
	private boolean topAdded;
	private boolean isCTE;

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

		// checks the query buffer for CTE queries or simple SELECT statements
		// returns the index where the injection should start.
		final int offset = getStatementIndex( sb );

		if ( !LimitHelper.hasFirstRow( selection ) ) {
			addTopExpression( sb, offset );
		}
		else {
			final String selectClause = fillAliasInSelectClause( sb, offset );

			if ( shallowIndexOfPattern( sb, ORDER_BY_PATTERN, offset ) > 0 ) {
				// ORDER BY requires using TOP
				addTopExpression( sb, offset );
			}

			encloseWithOuterQuery( sb, offset );

			sb.insert( offset, !isCTE ? "WITH query AS (" : ", query AS (" );
			sb.append( ") SELECT " ).append( selectClause ).append( " FROM query " );
			sb.append( "WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?" );
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
	 * @param offset the starting offset.
	 *
	 * @return List of aliases separated with comas or {@literal *}.
	 */
	protected String fillAliasInSelectClause(StringBuilder sb, int offset) {
		final String separator = System.lineSeparator();
		final List<String> aliases = new LinkedList<String>();
		final int startPos = getSelectColumnsStartPosition( sb, offset );
		int endPos = shallowIndexOfPattern( sb, FROM_PATTERN, startPos );

		int nextComa = startPos;
		int prevComa = startPos;
		int unique = 0;
		boolean selectsMultipleColumns = false;

		while ( nextComa != -1 ) {
			prevComa = nextComa;
			nextComa = shallowIndexOfPattern( sb, COMMA_PATTERN, nextComa );
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
		endPos = shallowIndexOfPattern( sb, FROM_PATTERN, startPos );
		final String expression = sb.substring( prevComa, endPos );
		if ( selectsMultipleColumns( expression ) ) {
			selectsMultipleColumns = true;
		}
		else {
			String alias = getAlias( expression );
			if ( alias == null ) {
				// Inserting alias. It is unlikely that we would have to add alias, but just in case.
				alias = StringHelper.generateAlias( "page", unique );
				final boolean endWithSeparator = sb.substring( endPos - separator.length() ).startsWith( separator );
				sb.insert( endPos - ( endWithSeparator ? 2 : 1 ), " as " + alias );
			}
			aliases.add( alias );
		}

		// In case of '*' or '{table}.*' expressions adding an alias breaks SQL syntax, returning '*'.
		return selectsMultipleColumns ? "*" : String.join( ", ", aliases );
	}

	/**
	 * Get the start position for where the column list begins.
	 *
	 * @param sb the string builder sql.
	 * @param offset the starting offset.
	 * @return the start position where the column list begins.
	 */
	private int getSelectColumnsStartPosition(StringBuilder sb, int offset) {
		final int startPos = getSelectStartPosition( sb, offset );
		// adjustment for 'select distinct ' and 'select '.
		final String sql = sb.toString().substring( startPos ).toLowerCase();
		if ( sql.startsWith( SELECT_DISTINCT_SPACE ) ) {
			return ( startPos + SELECT_DISTINCT_SPACE.length() );
		}
		else if ( sql.startsWith( SELECT_SPACE ) ) {
			return ( startPos + SELECT_SPACE.length() );
		}
		return startPos;
	}

	/**
	 * Get the select start position.
	 *
	 * @param sb the string builder sql.
	 * @param offset the starting offset in buffer.
	 * @return the position where {@code select} is found.
	 */
	private int getSelectStartPosition(StringBuilder sb, int offset) {
		return shallowIndexOfPattern( sb, SELECT_PATTERN, offset );
	}

	/**
	 * @param expression Select expression.
	 *
	 * @return {@code true} when expression selects multiple columns, {@code false} otherwise.
	 */
	private boolean selectsMultipleColumns(String expression) {
		final String lastExpr = expression.trim().replaceFirst( "(?i)(.)*\\s", "" ).trim();
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
		// remove any function arguments, if any exist.
		// 'cast(tab1.col1 as varchar(255)) as col1' -> 'cast as col1'
		// 'cast(tab1.col1 as varchar(255)) col1 -> 'cast col1'
		// 'cast(tab1.col1 as varchar(255))' -> 'cast'
		expression = expression.replaceFirst( "(\\((.)*\\))", "" ).trim();

		// This will match any text provided with:
		// 		columnName [[as] alias]
		final Matcher matcher = ALIAS_PATTERN.matcher( expression );

		String alias = null;
		if ( matcher.find() && matcher.groupCount() > 1 ) {
			// default to the alias after 'as' if detected
			alias = matcher.group( 3 );
			if ( alias == null ) {
				// use the clause which has on proceeding 'as' fragment.
				alias = matcher.group( 0 );
			}
		}

		return ( alias != null ? alias.trim() : null );
	}

	/**
	 * Encloses original SQL statement with outer query that provides {@literal __hibernate_row_nr__} column.
	 *
	 * @param sql SQL query.
	 * @param offset SQL query offset.
	 */
	protected void encloseWithOuterQuery(StringBuilder sql, int offset) {
		sql.insert( offset, "SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " );
		sql.append( " ) inner_query " );
	}

	/**
	 * Adds {@code TOP} expression. Parameter value is bind in
	 * {@link #bindLimitParametersAtStartOfQuery(RowSelection, PreparedStatement, int)} method.
	 *
	 * @param sql SQL query.
	 * @param offset the offset where top expression pattern matching should begin.
	 */
	protected void addTopExpression(StringBuilder sql, int offset) {
		// We should use either of these which come first (SELECT or SELECT DISTINCT).
		final int selectPos = shallowIndexOfPattern( sql, SELECT_PATTERN, offset );
		final int selectDistinctPos = shallowIndexOfPattern( sql, SELECT_DISTINCT_PATTERN, offset );
		if ( selectPos == selectDistinctPos ) {
			// Place TOP after SELECT DISTINCT
			sql.insert( selectDistinctPos + SELECT_DISTINCT.length(), " TOP(?)" );
		}
		else {
			// Place TOP after SELECT
			sql.insert( selectPos + SELECT.length(), " TOP(?)" );
		}
		topAdded = true;
	}

	/**
	 * Returns index of the first case-insensitive match of search pattern that is not
	 * enclosed in parenthesis.
	 *
	 * @param sb String to search.
	 * @param pattern Compiled search pattern.
	 * @param fromIndex The index from which to start the search.
	 *
	 * @return Position of the first match, or {@literal -1} if not found.
	 */
	private static int shallowIndexOfPattern(final StringBuilder sb, final Pattern pattern, int fromIndex) {
		int index = -1;
		final String matchString = sb.toString();

		// quick exit, save performance and avoid exceptions
		if ( matchString.length() < fromIndex || fromIndex < 0 ) {
			return -1;
		}

		List<IgnoreRange> ignoreRangeList = generateIgnoreRanges( matchString );

		Matcher matcher = pattern.matcher( matchString );
		matcher.region( fromIndex, matchString.length() );

		if ( ignoreRangeList.isEmpty() ) {
			// old behavior where the first match is used if no ignorable ranges
			// were deduced from the matchString.
			if ( matcher.find() && matcher.groupCount() > 0 ) {
				index = matcher.start();
			}
		}
		else {
			// rather than taking the first match, we now iterate all matches
			// until we determine a match that isn't considered "ignorable'.
			while ( matcher.find() && matcher.groupCount() > 0 ) {
				final int position = matcher.start();
				if ( !isPositionIgnorable( ignoreRangeList, position ) ) {
					index = position;
					break;
				}
			}
		}
		return index;
	}

	/**
	 * Builds a pattern that can be used to find matches of case-insensitive matches
	 * based on the search pattern that is not enclosed in parenthesis.
	 *
	 * @param pattern String search pattern.
	 * @param wordBoundary whether to apply a word boundary restriction.
	 * @return Compiled {@link Pattern}.
	 */
	private static Pattern buildShallowIndexPattern(String pattern, boolean wordBoundary) {
		return Pattern.compile(
				"(" +
				( wordBoundary ? "\\b" : "" ) +
				pattern +
				( wordBoundary ? "\\b" : "" ) +
				")(?![^\\(|\\[]*(\\)|\\]))",
				Pattern.CASE_INSENSITIVE
		);
	}

	/**
	 * Generates a list of {@code IgnoreRange} objects that represent nested sections of the
	 * provided SQL buffer that should be ignored when performing regular expression matches.
	 *
	 * @param sql The SQL buffer.
	 * @return list of {@code IgnoreRange} objects, never {@code null}.
	 */
	private static List<IgnoreRange> generateIgnoreRanges(String sql) {
		List<IgnoreRange> ignoreRangeList = new ArrayList<IgnoreRange>();

		int depth = 0;
		int start = -1;
		boolean insideAStringValue = false;
		for ( int i = 0; i < sql.length(); ++i ) {
			final char ch = sql.charAt( i );
			if ( ch == '\'' ) {
				insideAStringValue = !insideAStringValue;
			}
			else if ( ch == '(' && !insideAStringValue ) {
				depth++;
				if ( depth == 1 ) {
					start = i;
				}
			}
			else if ( ch == ')' && !insideAStringValue ) {
				if ( depth > 0 ) {
					if ( depth == 1 ) {
						ignoreRangeList.add( new IgnoreRange( start, i ) );
						start = -1;
					}
					depth--;
				}
				else {
					throw new IllegalStateException( "Found an unmatched ')' at position " + i + ": " + sql );
				}
			}
		}

		if ( depth != 0 ) {
			throw new IllegalStateException( "Unmatched parenthesis in rendered SQL (" + depth + " depth): " + sql );
		}

		return ignoreRangeList;
	}

	/**
	 * Returns whether the specified {@code position} is within the ranges of the {@code ignoreRangeList}.
	 *
	 * @param ignoreRangeList list of {@code IgnoreRange} objects deduced from the SQL buffer.
	 * @param position the position to determine whether is ignorable.
	 * @return {@code true} if the position is to ignored/skipped, {@code false} otherwise.
	 */
	private static boolean isPositionIgnorable(List<IgnoreRange> ignoreRangeList, int position) {
		for ( IgnoreRange ignoreRange : ignoreRangeList ) {
			if ( ignoreRange.isWithinRange( position ) ) {
				return true;
			}
		}
		return false;
	}

	static class IgnoreRange {
		private int start;
		private int end;

		IgnoreRange(int start, int end) {
			this.start = start;
			this.end = end;
		}

		boolean isWithinRange(int position) {
			return position >= start && position <= end;
		}
	}

	/**
	 * Get the starting point for the limit handler to begin injecting and transforming the SQL.
	 * For non-CTE queries, this is offset 0.  For CTE queries, this will be where the CTE's
	 * SELECT clause begins (skipping all query definitions, column definitions and expressions).
	 *
	 * This method also sets {@code isCTE} if the query is parsed as a CTE query.
	 *
	 * @param sql The sql buffer.
	 * @return the index where to begin parsing.
	 */
	private int getStatementIndex(StringBuilder sql) {
		final Matcher matcher = WITH_CTE.matcher( sql.toString() );
		if ( matcher.find() && matcher.groupCount() > 0 ) {
			isCTE = true;
			return locateQueryInCTEStatement( sql, matcher.end() );
		}
		return 0;
	}

	/**
	 * Steps through the SQL buffer from the specified offset and performs a series of pattern matches.
	 * The method locates where the CTE SELECT clause begins and returns that offset from the SQL buffer.
	 *
	 * @param sql The sql buffer.
	 * @param offset The offset to begin pattern matching.
	 *
	 * @return the offset where the CTE SELECT clause begins.
	 * @throws IllegalArgumentException if the parse of the CTE query fails.
	 */
	private int locateQueryInCTEStatement(StringBuilder sql, int offset) {
		while ( true ) {
			Matcher matcher = WITH_EXPRESSION_NAME.matcher( sql.substring( offset ) );
			if ( matcher.find() && matcher.groupCount() > 0 ) {
				offset += matcher.end();
				matcher = WITH_COLUMN_NAMES_START.matcher( sql.substring( offset ) );
				if ( matcher.find() && matcher.groupCount() > 0 ) {
					offset += matcher.end();
					matcher = WITH_COLUMN_NAMES_END.matcher( sql.substring( offset ) );
					if ( matcher.find() && matcher.groupCount() > 0 ) {
						offset += matcher.end();
						offset += advanceOverCTEInnerQuery( sql, offset );
						matcher = WITH_COMMA.matcher( sql.substring( offset ) );
						if ( matcher.find() && matcher.groupCount() > 0 ) {
							// another CTE fragment exists, re-start parse of CTE
							offset += matcher.end();
						}
						else {
							// last CTE fragment, we're at the start of the SQL.
							return offset;
						}
					}
					else {
						throw new IllegalArgumentException(
								String.format(
										Locale.ROOT,
										"Failed to parse CTE expression columns at offset %d, SQL [%s]",
										offset,
										sql.toString()
								)
						);
					}
				}
				else {
					matcher = WITH_AS.matcher( sql.substring( offset ) );
					if ( matcher.find() && matcher.groupCount() > 0 ) {
						offset += matcher.end();
						offset += advanceOverCTEInnerQuery( sql, offset );
						matcher = WITH_COMMA.matcher( sql.substring( offset ) );
						if ( matcher.find() && matcher.groupCount() > 0 ) {
							// another CTE fragment exists, re-start parse of CTE
							offset += matcher.end();
						}
						else {
							// last CTE fragment, we're at the start of the SQL.
							return offset;
						}
					}
					else {
						throw new IllegalArgumentException(
								String.format(
										Locale.ROOT,
										"Failed to locate AS keyword in CTE query at offset %d, SQL [%s]",
										offset,
										sql.toString()
								)
						);
					}
				}
			}
			else {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Failed to locate CTE expression name at offset %d, SQL [%s]",
								offset,
								sql.toString()
						)
				);
			}
		}
	}

	/**
	 * Advances over the CTE inner query that is contained inside matching '(' and ')'.
	 *
	 * @param sql The sql buffer.
	 * @param offset The offset where to begin advancing the position from.
	 * @return the position immediately after the CTE inner query plus 1.
	 *
	 * @throws IllegalArgumentException if the matching parenthesis aren't detected at the end of the parse.
	 */
	private int advanceOverCTEInnerQuery(StringBuilder sql, int offset) {
		int brackets = 0;
		int index = offset;
		boolean inString = false;
		for ( ; index < sql.length(); ++index ) {
			if ( sql.charAt( index ) == '\'' && !inString ) {
				inString = true;
			}
			else if ( sql.charAt( index ) == '\'' && inString ) {
				inString = false;
			}
			else if ( sql.charAt( index ) == '(' && !inString ) {
				brackets++;
			}
			else if ( sql.charAt( index ) == ')' && !inString ) {
				brackets--;
				if ( brackets == 0 ) {
					break;
				}
			}
		}

		if ( brackets > 0 ) {
			throw new IllegalArgumentException(
					"Failed to parse the CTE query inner query because closing ')' was not found."
			);
		}

		return index - offset + 1;
	}
}
