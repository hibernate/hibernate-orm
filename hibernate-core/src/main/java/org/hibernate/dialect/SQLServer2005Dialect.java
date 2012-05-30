/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect;

import java.sql.SQLException;
import java.sql.Types;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.QueryTimeoutException;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.type.StandardBasicTypes;

/**
 * A dialect for Microsoft SQL 2005. (HHH-3936 fix)
 *
 * @author Yoryos Valotasios
 */
public class SQLServer2005Dialect extends SQLServerDialect {
	private static final String SELECT = "select";
	private static final String FROM = "from";
	private static final String DISTINCT = "distinct";
	private static final String ORDER_BY = "order by";
	private static final int MAX_LENGTH = 8000;

	/**
	 * Regular expression for stripping alias
	 */
	private static final Pattern ALIAS_PATTERN = Pattern.compile( "\\sas\\s[^,]+(,?)" );

	public SQLServer2005Dialect() {
		// HHH-3965 fix
		// As per http://www.sql-server-helper.com/faq/sql-server-2005-varchar-max-p01.aspx
		// use varchar(max) and varbinary(max) instead of TEXT and IMAGE types
		registerColumnType( Types.BLOB, "varbinary(MAX)" );
		registerColumnType( Types.VARBINARY, "varbinary(MAX)" );
		registerColumnType( Types.VARBINARY, MAX_LENGTH, "varbinary($l)" );
		registerColumnType( Types.LONGVARBINARY, "varbinary(MAX)" );

		registerColumnType( Types.CLOB, "varchar(MAX)" );
		registerColumnType( Types.LONGVARCHAR, "varchar(MAX)" );
		registerColumnType( Types.VARCHAR, "varchar(MAX)" );
		registerColumnType( Types.VARCHAR, MAX_LENGTH, "varchar($l)" );

		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.BOOLEAN, "bit" );


		registerFunction( "row_number", new NoArgSQLFunction( "row_number", StandardBasicTypes.INTEGER, true ) );
	}

	@Override
	public boolean supportsLimitOffset() {
		return true;
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return false;
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

	@Override
	public String getLimitString(String query, int offset, int limit) {
		// We transform the query to one with an offset and limit if we have an offset and limit to bind
		if ( offset > 1 || limit > 1 ) {
			return getLimitString( query, true );
		}
		return query;
	}

	/**
	 * Add a LIMIT clause to the given SQL SELECT (HHH-2655: ROW_NUMBER for Paging)
	 *
	 * The LIMIT SQL will look like:
	 *
	 * <pre>
	 * WITH query AS (
	 *   original_select_clause_without_distinct_and_order_by,
	 *   ROW_NUMBER() OVER ([ORDER BY CURRENT_TIMESTAMP | original_order_by_clause]) as __hibernate_row_nr__
	 *   original_from_clause
	 *   original_where_clause
	 *   group_by_if_originally_select_distinct
	 * )
	 * SELECT * FROM query WHERE __hibernate_row_nr__ >= offset AND __hibernate_row_nr__ < offset + last
	 * </pre>
	 *
	 * @param querySqlString The SQL statement to base the limit query off of.
	 * @param hasOffset Is the query requesting an offset?
	 *
	 * @return A new SQL statement with the LIMIT clause applied.
	 */
	@Override
	public String getLimitString(String querySqlString, boolean hasOffset) {
		StringBuilder sb = new StringBuilder( querySqlString.trim() );

		int orderByIndex = shallowIndexOfWord( sb, ORDER_BY, 0 );
		CharSequence orderby = orderByIndex > 0 ? sb.subSequence( orderByIndex, sb.length() )
				: "ORDER BY CURRENT_TIMESTAMP";

		// Delete the order by clause at the end of the query
		if ( orderByIndex > 0 ) {
			sb.delete( orderByIndex, orderByIndex + orderby.length() );
		}

		// HHH-5715 bug fix
		replaceDistinctWithGroupBy( sb );

		insertRowNumberFunction( sb, orderby );

		// Wrap the query within a with statement:
		sb.insert( 0, "WITH query AS (" ).append( ") SELECT * FROM query " );
		sb.append( "WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?" );

		return sb.toString();
	}

	/**
	 * Utility method that checks if the given sql query is a select distinct one and if so replaces the distinct select
	 * with an equivalent simple select with a group by clause.
	 *
	 * @param sql an sql query
	 */
	protected static void replaceDistinctWithGroupBy(StringBuilder sql) {
		int distinctIndex = shallowIndexOfWord( sql, DISTINCT, 0 );
		int selectEndIndex = shallowIndexOfWord( sql, FROM, 0 );
		if (distinctIndex > 0 && distinctIndex < selectEndIndex) {
			sql.delete( distinctIndex, distinctIndex + DISTINCT.length() + " ".length());
			sql.append( " group by" ).append( getSelectFieldsWithoutAliases( sql ) );
		}
	}

	public static final String SELECT_WITH_SPACE = SELECT + ' ';

	/**
	 * This utility method searches the given sql query for the fields of the select statement and returns them without
	 * the aliases.
	 *
	 * @param sql sql query
	 *
	 * @return the fields of the select statement without their alias
	 */
	protected static CharSequence getSelectFieldsWithoutAliases(StringBuilder sql) {
		final int selectStartPos = shallowIndexOf( sql, SELECT_WITH_SPACE, 0 );
		final int fromStartPos = shallowIndexOfWord( sql, FROM, selectStartPos );
		String select = sql.substring( selectStartPos + SELECT.length(), fromStartPos );

		// Strip the as clauses
		return stripAliases( select );
	}

	/**
	 * Utility method that strips the aliases.
	 *
	 * @param str string to replace the as statements
	 *
	 * @return a string without the as statements
	 */
	protected static String stripAliases(String str) {
		Matcher matcher = ALIAS_PATTERN.matcher( str );
		return matcher.replaceAll( "$1" );
	}

	/**
	 * We must place the row_number function at the end of select clause.
	 *
	 * @param sql the initial sql query without the order by clause
	 * @param orderby the order by clause of the query
	 */
	protected void insertRowNumberFunction(StringBuilder sql, CharSequence orderby) {
		// Find the end of the select clause
		int selectEndIndex = shallowIndexOfWord( sql, FROM, 0 );

		// Insert after the select clause the row_number() function:
		sql.insert( selectEndIndex - 1, ", ROW_NUMBER() OVER (" + orderby + ") as __hibernate_row_nr__" );
	}

	@Override // since SQLServer2005 the nowait hint is supported
	public String appendLockHint(LockMode mode, String tableName) {
		if ( mode == LockMode.UPGRADE_NOWAIT ) {
			return tableName + " with (updlock, rowlock, nowait)";
		}
		return super.appendLockHint( mode, tableName );
	}

	/**
	 * Returns index of the first case-insensitive match of search term surrounded by spaces
	 * that is not enclosed in parentheses.
	 *
	 * @param sb String to search.
	 * @param search Search term.
	 * @param fromIndex The index from which to start the search.
	 * @return Position of the first match, or {@literal -1} if not found.
	 */
	private static int shallowIndexOfWord(final StringBuilder sb, final String search, int fromIndex) {
		final int index = shallowIndexOf( sb, ' ' + search + ' ', fromIndex );
		return index != -1 ? ( index + 1 ) : -1; // In case of match adding one because of space placed in front of search term.
	}

	/**
	 * Returns index of the first case-insensitive match of search term that is not enclosed in parentheses.
	 *
	 * @param sb String to search.
	 * @param search Search term.
	 * @param fromIndex The index from which to start the search.
	 * @return Position of the first match, or {@literal -1} if not found.
	 */
	private static int shallowIndexOf(StringBuilder sb, String search, int fromIndex) {
		final String lowercase = sb.toString().toLowerCase(); // case-insensitive match
		final int len = lowercase.length();
		final int searchlen = search.length();
		int pos = -1, depth = 0, cur = fromIndex;
		do {
			pos = lowercase.indexOf( search, cur );
			if ( pos != -1 ) {
				for ( int iter = cur; iter < pos; iter++ ) {
					char c = sb.charAt( iter );
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
	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );

				if(  "HY008".equals( sqlState )){
					throw new QueryTimeoutException( message, sqlException, sql );
				}
				return null;
			}
		};
	}

}
