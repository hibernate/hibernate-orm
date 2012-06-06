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
import org.hibernate.LockOptions;
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
	private static final String AS = " as ";
	private static final int MAX_LENGTH = 8000;

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
		if (sb.charAt(sb.length() - 1) == ';') {
			sb.setLength(sb.length() - 1);
		}

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
		sb.insert( 0, "WITH query AS (" ).append( ") SELECT" + getMainSelectClauseIds(new StringBuilder( querySqlString ), true) + " FROM query " );
		sb.append( "WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?" );

		return sb.toString();
	}

	/**
	 * Facility method that either returns alias-based or not alias-based parameters of principal select-clause
	 * remark: is able to recognize sub-selects (= nested selects enclosed in parentheses)
	 * @param sql
	 * @param aliases 
	 * @return aliases == true:  returns aliases (or original expression if no alias is specified) of the main select-clause  
	 *         aliases == false: returns original expressions stripped of all aliases 
	 */
	protected static String getMainSelectClauseIds(StringBuilder sb, boolean aliases) {
		int startPos = shallowIndexOf( sb, SELECT_WITH_SPACE, 0 );
		int endPos = shallowIndexOfWord( sb, FROM, startPos );
		StringBuilder completeSelectClause = new StringBuilder(sb.substring(startPos + SELECT_WITH_SPACE.length() - 1, endPos - startPos).trim());
		
		StringBuilder ids = new StringBuilder();
		int index = 0;
		// processing comma separated elements skipping those enclosed in parentheses
		while (true) {
			int nextcolon = shallowIndexOf( completeSelectClause, ",", index );
			if (nextcolon < 0) {
				break;
			}
			
			String id;
			if (aliases) {
				int startId = completeSelectClause.substring(index, nextcolon).trim().lastIndexOf(' ') + 1;
				id = completeSelectClause.substring(index + startId, nextcolon);
			}
			else {
				int endId = completeSelectClause.substring(index, nextcolon).trim().lastIndexOf(' ') + 1;
				if (endId == 0) {
					endId = nextcolon;
				}
				id = completeSelectClause.substring(index, index + endId) + " ";
				if (id.toLowerCase().contains(AS)) {
					id = id.substring(0, id.toLowerCase().lastIndexOf(AS));
				}
			}
			ids.append(id.trim() + ", ");
			index = nextcolon + 1;
		}
		// processing last element (= not followed by a colon)
		String id;
		if (aliases) {
			int startId = completeSelectClause.lastIndexOf(" ");
			if (startId < 0) {
				startId = 0;
			}
			id = completeSelectClause.substring(startId);
		}
		else {
			id = completeSelectClause.substring(index);
			String tolower = id.toLowerCase() + " ";
			if (tolower.contains(AS)) {
				id = id.substring(0, tolower.lastIndexOf(AS));
			}
			else {
				String trimmed = id.trim();
				if (trimmed.contains(" ")) { 
					// assuming this being an implicit alias without AS keyword
					id = trimmed.substring(0, trimmed.lastIndexOf(" "));
				}
			}
		}
		ids.append(id.trim());
		return " " + ids.toString();
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
			sql.append( " group by" ).append( getMainSelectClauseIds(sql, false));
		}
	}

	public static final String SELECT_WITH_SPACE = SELECT + ' ';

//	buggy, as the "AS" keyword is optional and thus may not be present at all
//	protected static String stripAliases(String str) {
//		Matcher matcher = ALIAS_PATTERN.matcher( str );
//		return matcher.replaceAll( "$1" );
//	}

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
	public String appendLockHint(LockOptions lockOptions, String tableName) {
		if ( lockOptions.getLockMode() == LockMode.UPGRADE_NOWAIT ) {
			return tableName + " with (updlock, rowlock, nowait)";
		}
		LockMode mode = lockOptions.getLockMode();
		boolean isNoWait = lockOptions.getTimeOut() == LockOptions.NO_WAIT;
		String noWaitStr = isNoWait? ", nowait" :"";
		switch ( mode ) {
			case UPGRADE_NOWAIT:
				 return tableName + " with (updlock, rowlock, nowait)";
			case UPGRADE:
			case PESSIMISTIC_WRITE:
			case WRITE:
				return tableName + " with (updlock, rowlock"+noWaitStr+" )";
			case PESSIMISTIC_READ:
				return tableName + " with (holdlock, rowlock"+noWaitStr+" )";
			default:
				return tableName;
		}
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
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
				if(  "HY008".equals( sqlState )){
					throw new QueryTimeoutException( message, sqlException, sql );
				}
				if (1222 == errorCode ) {
					throw new LockTimeoutException( message, sqlException, sql );
				}
				return null;
			}
		};
	}



}
