//$Id: SQLServerDialect.java 11303 2007-03-19 22:06:14Z steve.ebersole@jboss.com $
package org.hibernate.dialect;

import java.sql.Types;
import java.util.Map;
import java.util.Iterator;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.util.StringHelper;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.AnsiTrimEmulationFunction;

/**
 * A dialect for Microsoft SQL Server 2000 and 2005
 *
 * @author Gavin King
 */
public class SQLServerDialect extends SybaseDialect {

	public SQLServerDialect() {
		registerColumnType( Types.VARBINARY, "image" );
		registerColumnType( Types.VARBINARY, 8000, "varbinary($l)" );

		registerFunction( "second", new SQLFunctionTemplate( Hibernate.INTEGER, "datepart(second, ?1)" ) );
		registerFunction( "minute", new SQLFunctionTemplate( Hibernate.INTEGER, "datepart(minute, ?1)" ) );
		registerFunction( "hour", new SQLFunctionTemplate( Hibernate.INTEGER, "datepart(hour, ?1)" ) );
		registerFunction( "locate", new StandardSQLFunction( "charindex", Hibernate.INTEGER ) );

		registerFunction( "extract", new SQLFunctionTemplate( Hibernate.INTEGER, "datepart(?1, ?3)" ) );
		registerFunction( "mod", new SQLFunctionTemplate( Hibernate.INTEGER, "?1 % ?2" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( Hibernate.INTEGER, "datalength(?1) * 8" ) );

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

	public String getLimitString(String querySelect, int offset, int limit) {
		if ( offset > 0 ) {
			throw new UnsupportedOperationException( "sql server has no offset" );
		}
		return new StringBuffer( querySelect.length() + 8 )
				.append( querySelect )
				.insert( getAfterSelectInsertPoint( querySelect ), " top " + limit )
				.toString();
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
		if ( mode.greaterThan( LockMode.READ ) ) {
			// does this need holdlock also? : return tableName + " with (updlock, rowlock, holdlock)";
			return tableName + " with (updlock, rowlock)";
		}
		else {
			return tableName;
		}
	}

	public String getSelectGUIDString() {
		return "select newid()";
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
		// cascade delete constraints which can circel back to the mutating
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
