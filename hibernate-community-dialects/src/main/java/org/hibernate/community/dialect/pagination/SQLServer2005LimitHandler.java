/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.dialect.pagination.spi.AbstractLimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationJdbcInstructions;
import org.hibernate.dialect.pagination.spi.PaginationRequest;
import org.hibernate.dialect.pagination.spi.PaginationResult;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

/**
 * A {@link LimitHandler} compatible with SQL Server 2005 and later
 * that uses {@code top()} and {@code rownumber()}.
 *
 * @author Lukasz Antoniak
 * @author Chris Cranford
 * @author Gavin King
 */
public class SQLServer2005LimitHandler extends AbstractLimitHandler {

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean useMaxForLimit() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return true;
	}

	@Override
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		// Our dialect paginated results aren't zero based.
		// The first row should get the number 1 and so on
		return zeroBasedFirstResult + 1;
	}

	/**
	 * When the offset of the current row is {@literal 0},
	 * add a {@code top(?)} clause to the given SQL query. When the offset
	 * is non-zero, wrap the given query in an outer query that limits the
	 * results using the {@code row_number()} window function.
	 *
	 * <pre>
	 * with query_ as (
	 *     select row_.*, row_number()
	 *         over (order by current_timestamp) AS rownumber_
	 *     from ( [original-query] ) row_
	 * )
	 * select [alias-list] from query_
	 * where rownumber_ &gt;= ? and rownumber_ &lt; ?
	 * </pre>
	 * <p>
	 * Where {@code [original-query]} is the original SQL query, with a
	 * {@code top()} clause added iff the query has an {@code order by}
	 * clause, and with generated aliases added to any elements of the
	 * projection list that don't already have aliases, and
	 * {@code [alias-list]} is a list of aliases in the projection list.
	 *
	 * @return A new SQL statement
	 */
	@Override
	public PaginationResult processSql(PaginationRequest request) {
		if ( request.isEmpty() ) {
			return PaginationResult.unchanged( request.sql() );
		}
		String sql = request.sql().trim();
		if ( sql.endsWith(";") ) {
			sql = sql.substring( 0, sql.length()-1 );
		}

		final int selectOffset = Keyword.SELECT.rootOffset( sql );
		final int afterSelectOffset = Keyword.SELECT.endOffset( sql, selectOffset );
		final int fromOffset = Keyword.FROM.rootOffset( sql ); //TODO: what if there is no 'from' clause?!

		boolean hasCommonTables = Keyword.WITH.occursAt( sql, 0 );
		boolean hasOrderBy = Keyword.ORDER_BY.rootOffset( sql ) > 0;
		boolean hasFirstRow = request.hasFirstRow();
		boolean topAdded = !hasFirstRow || hasOrderBy;

		final StringBuilder result = new StringBuilder( sql );

		if ( !hasFirstRow || hasOrderBy ) {
			if ( request.usesStandardParameterMarkers() ) {
				result.insert( afterSelectOffset, " top(?)" );
			}
			else {
				final String parameterMarker = request.parameterMarker( 1 );
				result.insert( afterSelectOffset, " top(" + parameterMarker + ")" );
			}
		}

		if ( hasFirstRow ) {

			// enclose original SQL statement with outer query
			// that provides the rownumber_ column

			String aliases = selectAliases( sql, afterSelectOffset, fromOffset, result ); //warning: changes result by side-effect
			result.insert( selectOffset, ( hasCommonTables ? "," : "with" )
					+ " query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" )
					.append( ") row_) select " ).append( aliases )
					.append( " from query_ where rownumber_>=" );
			if ( request.usesStandardParameterMarkers() ) {
				result.append( "? and rownumber_<?" );
			}
			else {
				final int firstPosition = request.jdbcParameterCount() + 1 + ( topAdded ? 1 : 0 );
				result.append( request.parameterMarker( firstPosition ) );
				result.append( " and rownumber_<" );
				result.append( request.parameterMarker( firstPosition + 1 ) );
			}
		}

		final List<Integer> start = topAdded
				? List.of( maxOrLimit( request ) - 1 )
				: List.of();
		final List<Integer> end = hasFirstRow
				? List.of( getFirstRow( request ), maxOrLimit( request ) )
				: List.of();
		return new PaginationResult(
				result.toString(),
				new PaginationJdbcInstructions( start, end, null, 0 )
		);
	}

	@Override
	public int parameterPositionStart(PaginationRequest request) {
		return !request.isEmpty()
				&& ( !request.hasFirstRow() || Keyword.ORDER_BY.rootOffset( request.sql() ) > 0 )
				? 2
				: 1;
	}

	private int maxOrLimit(PaginationRequest request) {
		return request.hasMaxRows() ? getMaxOrLimit( request ) : Integer.MAX_VALUE;
	}

	/**
	 * Add any missing aliases to the given {@code select} list, and return
	 * comma-separated list of all aliases in the list, unless the given list
	 * contains {@literal *} or {@literal {table.*}}, in which case {@literal *}
	 * is returned.
	 *
	 * @param sql the whole query
	 * @param afterSelectOffset the starting offset of the select list
	 * @param fromOffset the ending offset of the select list
	 * @param result a string builder for inserting missing aliases
	 *
	 * @return a comma-separated list of aliases, or {@literal *}
	 *
	 */
	private String selectAliases(String sql, int afterSelectOffset, int fromOffset, StringBuilder result) {
		final List<String> aliases = new LinkedList<>();
		int unique = 0;

		int offset = afterSelectOffset;
		do {
			int nextOffset = nextElement( sql, offset, fromOffset );
			String selectElement = sql.substring( offset, nextOffset );
			int asIndex = Keyword.AS.rootOffset( selectElement );
			String expression;
			String alias;
			if ( asIndex == 0 ) {
				expression = selectElement.trim();
				if (expression.equals( "*" ) || expression.endsWith( ".*" )) {
					alias = "";
				}
				else {
					int aliasIndex = getAliasIndex( expression );
					if ( aliasIndex == -1 ) {
						//no alias found, need to generate and insert it!
						alias = generateAlias( unique++ );
						int diff = result.length() - sql.length();
						if ( result.charAt( nextOffset + diff - 1 ) == ' ' ) {
							diff--;
						}
						result.insert( nextOffset + diff, " as " + alias );
					}
					else {
						alias = expression.substring( aliasIndex ).trim();
						expression = expression.substring( 0, aliasIndex ).trim();
					}
				}
			}
			else {
				expression = selectElement.substring( 0, asIndex ).trim();
				alias = selectElement.substring( asIndex+2 ).trim();
			}
			aliases.add( alias );
			if ( expression.endsWith("*") ) {
				// in case of '*' or 'table.*' expressions adding
				// an alias breaks SQL syntax, so just return '*'
				return "*";
			}
			offset = nextOffset + 1;
		}
		while ( offset < fromOffset );

		return String.join( ",", aliases );
	}

	private static String generateAlias(int unique) {
		return "col" + unique + '_';
	}

	private int getAliasIndex(String sql) {
		int endOffset = -1;
		int depth = 0;
		boolean quoted = false;
		boolean doubleQuoted = false;
		for ( int offset = sql.length() - 1; offset > endOffset; ) {
			int nextQuote = sql.lastIndexOf('\'', offset);
			if ( nextQuote < 0 || nextQuote < endOffset ) {
				nextQuote = endOffset;
			}
			if ( !quoted ) {
				for ( int index = offset; index > nextQuote; index-- ) {
					final char c = sql.charAt( index );
					switch ( c ) {
						case '(':
							depth--;
							break;
						case ')':
							depth++;
							break;
						case '"':
							doubleQuoted = !doubleQuoted;
							break;
						case '[':
							doubleQuoted = false;
							break;
						case ']':
							doubleQuoted = true;
							break;
						default:
							if ( Character.isWhitespace( c ) && depth == 0 && !doubleQuoted ) {
								return index + 1;
							}
					}
				}
			}
			quoted = !quoted;
			offset = nextQuote - 1;
		}
		return -1;
	}

	enum Keyword {

		SELECT ("select(\\s+(distinct|all))?"),
		FROM ("from"),
		ORDER_BY ("order\\s+by"),
		AS ("as"),
		WITH ("with");

		Pattern pattern;
		Keyword(String keyword) {
			pattern = compile( "^\\b" + keyword + "\\b", CASE_INSENSITIVE );
		}

		/**
		 * Look for a "root" occurrence of this keyword in
		 * the given SQL fragment, that is, an offset where
		 * the keyword occurs unquoted and not parenthesized.
		 *
		 * @param sql a fragment of SQL
		 * @return the offset at which the keyword occurs, or
		 *         0 if it never occurs outside of quotes or
		 *         parentheses.
		 */
		int rootOffset(String sql) {

			//TODO: does not handle comments

			//use a regex here for its magical ability
			//to match word boundaries and whitespace
			Matcher matcher = pattern.matcher( sql ).useTransparentBounds( true );

			int depth = 0;
			boolean quoted = false;
			boolean doubleQuoted = false;
			for ( int offset = 0, end = sql.length(); offset < end; ) {
				int nextQuote = sql.indexOf('\'', offset);
				if ( nextQuote<0 ) {
					nextQuote = end;
				}
				if ( !quoted ) {
					for ( int index=offset; index<nextQuote; index++ ) {
						switch ( sql.charAt( index ) ) {
							case '(':
								depth++;
								break;
							case ')':
								depth--;
								break;
							case '"':
								doubleQuoted = !doubleQuoted;
								break;
							case '[':
								doubleQuoted = true;
								break;
							case ']':
								doubleQuoted = false;
								break;
							default:
								if ( depth == 0 && !doubleQuoted ) {
									matcher.region( index, nextQuote );
									if ( matcher.find() ) {
										//we found the keyword!
										return index;
									}
								}
						}
					}
				}
				quoted = !quoted;
				offset = nextQuote + 1;
			}
			return 0; //none found
		}

		int endOffset(String sql, int startOffset) {
			Matcher matcher = pattern.matcher( sql ).useTransparentBounds( true );
			matcher.region( startOffset, sql.length() );
			matcher.find();
			return matcher.end();
		}

		boolean occursAt(String sql, int offset) {
			Matcher matcher = pattern.matcher( sql ).useTransparentBounds( true );
			matcher.region( offset, sql.length() );
			return matcher.find();
		}

	}

	/**
	 * Return the position just before the start of the next element
	 * of a comma-separated list, that is, the position of the next
	 * comma, or the position of the end of the list.
	 *
	 * @param sql A SQL fragment containing a comma-separated list
	 * @param startOffset the starting offset of the current element
	 * @param endOffset the end of the list
	 * @return the ending offset of the current element
	 */
	private int nextElement(String sql, int startOffset, int endOffset) {

		//TODO: does not handle comments

		int depth = 0;
		boolean quoted = false;
		boolean doubleQuoted = false;
		for ( int offset = startOffset; offset < endOffset; ) {
			int nextQuote = sql.indexOf('\'', offset);
			if ( nextQuote<0 || nextQuote>endOffset ) {
				nextQuote = endOffset;
			}
			if ( !quoted ) {
				for ( int index=offset; index<nextQuote; index++ ) {
					switch ( sql.charAt( index ) ) {
						case '(':
							depth++;
							break;
						case ')':
							depth--;
							break;
						case '"':
							doubleQuoted = !doubleQuoted;
							break;
						case '[':
							doubleQuoted = true;
							break;
						case ']':
							doubleQuoted = false;
							break;
						case ',':
							if ( depth == 0 && !doubleQuoted ) {
								return index;
							}
					}
				}
			}
			quoted = !quoted;
			offset = nextQuote + 1;
		}
		return endOffset;
	}

}
