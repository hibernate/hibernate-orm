/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.AnsiTrimEmulationFunction;

/**
 * A dialect for Microsoft SQL Server 2000 and 2005
 *
 * @author Gavin King
 */
public class SQLServerDialect extends AbstractTransactSQLDialect {

	public SQLServerDialect() {
		registerColumnType( Types.VARBINARY, "image" );
		registerColumnType( Types.VARBINARY, 8000, "varbinary($l)" );
		registerColumnType( Types.LONGVARBINARY, "image" );
		registerColumnType( Types.LONGVARCHAR, "text" );

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
			throw new UnsupportedOperationException( "query result offset is not supported" );
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
