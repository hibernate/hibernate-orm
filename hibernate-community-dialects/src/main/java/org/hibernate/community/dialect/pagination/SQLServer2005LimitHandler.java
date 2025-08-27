/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

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

	// records whether top() was added to the query
	private boolean topAdded;

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
	public String processSql(String sql, Limit limit) {
		return processSql( sql, -1, null, limit );
	}

	@Override
	public String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, QueryOptions queryOptions) {
		return processSql( sql, jdbcParameterCount, parameterMarkerStrategy, queryOptions.getLimit() );
	}

	private String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, @Nullable Limit limit) {
		sql = sql.trim();
		if ( sql.endsWith(";") ) {
			sql = sql.substring( 0, sql.length()-1 );
		}

		final int selectOffset = Keyword.SELECT.rootOffset( sql );
		final int afterSelectOffset = Keyword.SELECT.endOffset( sql, selectOffset );
		final int fromOffset = Keyword.FROM.rootOffset( sql ); //TODO: what if there is no 'from' clause?!

		boolean hasCommonTables = Keyword.WITH.occursAt( sql, 0 );
		boolean hasOrderBy = Keyword.ORDER_BY.rootOffset( sql ) > 0;
		boolean hasFirstRow = hasFirstRow( limit );

		final StringBuilder result = new StringBuilder( sql );

		if ( !hasFirstRow || hasOrderBy ) {
			if ( ParameterMarkerStrategyStandard.isStandardRenderer( parameterMarkerStrategy ) ) {
				result.insert( afterSelectOffset, " top(?)" );
			}
			else {
				final String parameterMarker = parameterMarkerStrategy.createMarker( 1, null );
				result.insert( afterSelectOffset, " top(" + parameterMarker + ")" );
			}
			topAdded = true;
		}

		if ( hasFirstRow ) {

			// enclose original SQL statement with outer query
			// that provides the rownumber_ column

			String aliases = selectAliases( sql, afterSelectOffset, fromOffset, result ); //warning: changes result by side-effect
			result.insert( selectOffset, ( hasCommonTables ? "," : "with" )
					+ " query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" )
					.append( ") row_) select " ).append( aliases )
					.append( " from query_ where rownumber_>=" );
			if ( ParameterMarkerStrategyStandard.isStandardRenderer( parameterMarkerStrategy ) ) {
				result.append( "? and rownumber_<?" );
			}
			else {
				final int firstPosition = jdbcParameterCount + 1 + ( topAdded ? 1 : 0 );
				result.append( parameterMarkerStrategy.createMarker( firstPosition, null ) );
				result.append( " and rownumber_<" );
				result.append( parameterMarkerStrategy.createMarker( firstPosition + 1, null ) );
			}
		}

		return result.toString();
	}

	@Override
	public int bindLimitParametersAtStartOfQuery(Limit limit, PreparedStatement statement, int index) throws SQLException {
		if ( topAdded ) {
			// bind parameter to top(?)
			statement.setInt( index, getMaxOrLimit( limit ) - 1 );
			return 1;
		}
		return 0;
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(Limit limit, PreparedStatement statement, int index) throws SQLException {
		return hasFirstRow( limit )
				? super.bindLimitParametersAtEndOfQuery( limit, statement, index )
				: 0;
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
						alias = StringHelper.generateAlias( "col", unique++ );
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
