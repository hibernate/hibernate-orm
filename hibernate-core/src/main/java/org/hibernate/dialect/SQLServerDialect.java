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

import java.sql.Types;

import org.hibernate.LockMode;
import org.hibernate.dialect.function.AnsiTrimEmulationFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * A dialect for Microsoft SQL Server 2000 and 2005
 *
 * @author Gavin King
 */
public class SQLServerDialect extends AbstractTransactSQLDialect {
	private static final String SELECT = "select";
    private static final String DISTINCT = "distinct";
    
    
	public SQLServerDialect() {
		registerColumnType( Types.VARBINARY, "image" );
		registerColumnType( Types.VARBINARY, 8000, "varbinary($l)" );
		registerColumnType( Types.LONGVARBINARY, "image" );
		registerColumnType( Types.LONGVARCHAR, "text" );

		registerFunction( "second", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(second, ?1)" ) );
		registerFunction( "minute", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(minute, ?1)" ) );
		registerFunction( "hour", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(hour, ?1)" ) );
		registerFunction( "locate", new StandardSQLFunction( "charindex", StandardBasicTypes.INTEGER ) );

		registerFunction( "extract", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(?1, ?3)" ) );
		registerFunction( "mod", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "?1 % ?2" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datalength(?1) * 8" ) );

		registerFunction( "trim", new AnsiTrimEmulationFunction() );

		registerKeyword( "top" );
	}

	public String getNoColumnsInsertString() {
		return "default values";
	}

	static int getAfterSelectInsertPoint(String sql) {
		int selectIndex = sql.toLowerCase().indexOf( "select" );
		final int selectDistinctIndex = sql.toLowerCase().indexOf( "select distinct" );
		return selectIndex + ( selectDistinctIndex == selectIndex ? 15 : 6 );
	}

	/**
	 * Add a LIMIT clause to the given SQL SELECT (HHH-2655: ROW_NUMBER for Paging)
	 *
	 * The LIMIT SQL will look like:
	 *
	 * <pre>
	 * WITH query AS (SELECT ROW_NUMBER() OVER (ORDER BY orderby) as __hibernate_row_nr__, original_query_without_orderby)
	 * SELECT * FROM query WHERE __hibernate_row_nr__ BEETWIN offset AND offset + last
	 * --ORDER BY __hibernate_row_nr__
	 * </pre>
	 *
	 * I don't think that the last order by clause is mandatory
	 *
	 * @param querySqlString The SQL statement to base the limit query off of.
	 * @param offset Offset of the first row to be returned by the query (zero-based)
	 * @param limit Maximum number of rows to be returned by the query
	 *
	 * @return A new SQL statement with the LIMIT clause applied.
	 */
	@Override
	public String getLimitString(String querySqlString, int offset, int limit) {
		if ( offset == 0 ) {
			return new StringBuffer( querySqlString.length() + 8 ).append( querySqlString )
					.insert( getAfterSelectInsertPoint( querySqlString ), " top " + limit )
					.toString();
		}

		StringBuilder sb = new StringBuilder( querySqlString.trim().toLowerCase() );

		int orderByIndex = sb.indexOf( "order by" );
		CharSequence orderby = orderByIndex > 0 ? sb.subSequence( orderByIndex, sb.length() ) : "ORDER BY CURRENT_TIMESTAMP";

		// Delete the order by clause at the end of the query
		if ( orderByIndex > 0 ) {
			sb.delete( orderByIndex, orderByIndex + orderby.length() );
		}

		replaceDistinctWithGroupBy( sb );
		
		insertRowNumberFunction(sb, orderby);
		
        //Wrap the query within a with statement:
        sb.insert(0, "WITH query AS (").append(") SELECT * FROM query ");
        sb.append("WHERE __hibernate_row_nr__ BETWEEN ").append(offset + 1).append(" AND ").append(limit);

        return sb.toString();
	}
	
	protected static void replaceDistinctWithGroupBy(StringBuilder sb) {
		int distinctIndex = sb.indexOf( DISTINCT );
		if (distinctIndex > 0) {
			
			sb.delete(distinctIndex, distinctIndex + DISTINCT.length() + 1);
			sb.append(" group by").append(getSelectFieldsWithoutAs(sb));
		}
	}

	protected static CharSequence getSelectFieldsWithoutAs(StringBuilder sql) {
		String select = sql.substring( sql.indexOf(SELECT) + SELECT.length(), sql.indexOf("from"));
		
		// Strip the as clauses
		return stripAsStatement(select);
	}

	protected static String stripAsStatement(String str) {
		return str.replaceAll("\\sas[^,]+(,?)", "$1");
	}
	
	protected static void insertRowNumberFunction(StringBuilder sb, CharSequence orderby) {
		// Find the end of the select statement
		int selectEndIndex = sb.indexOf( SELECT ) + SELECT.length();

        // Isert after the select statement the row_number() function:
        sb.insert( selectEndIndex, " ROW_NUMBER() OVER (" + orderby + ") as __hibernate_row_nr__," );
	}

	/**
	 * Use <tt>insert table(...) values(...) select SCOPE_IDENTITY()</tt>
	 */
	public String appendIdentitySelectToInsert(String insertSQL) {
		return insertSQL + " select scope_identity()";
	}

	public boolean supportsLimit() {
		return true;
	}

	public boolean useMaxForLimit() {
		return true;
	}

	public boolean supportsLimitOffset() {
		return true;
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return false;
	}

	public boolean supportsVariableLimit() {
		return false;
	}

	public char closeQuote() {
		return ']';
	}

	public char openQuote() {
		return '[';
	}

	public String appendLockHint(LockMode mode, String tableName) {
		if ( ( mode == LockMode.UPGRADE ) ||
				( mode == LockMode.UPGRADE_NOWAIT ) ||
				( mode == LockMode.PESSIMISTIC_WRITE ) ||
				( mode == LockMode.WRITE ) ) {
			return tableName + " with (updlock, rowlock)";
		}
		else if ( mode == LockMode.PESSIMISTIC_READ ) {
			return tableName + " with (holdlock, rowlock)";
		}
		else {
			return tableName;
		}
	}

	// The current_timestamp is more accurate, but only known to be supported
	// in SQL Server 7.0 and later (i.e., Sybase not known to support it at all)

	public String getCurrentTimestampSelectString() {
		return "select current_timestamp";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}

	public boolean supportsCircularCascadeDeleteConstraints() {
		// SQL Server (at least up through 2005) does not support defining
		// cascade delete constraints which can circle back to the mutating
		// table
		return false;
	}

	public boolean supportsLobValueChangePropogation() {
		// note: at least my local SQL Server 2005 Express shows this not working...
		return false;
	}

	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return false; // here assume SQLServer2005 using snapshot isolation, which does not have this problem
	}

	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return false; // here assume SQLServer2005 using snapshot isolation, which does not have this problem
	}

}
