//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Iterator;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CharIndexFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;

/**
 * An abstract base class for Sybase and MS SQL Server dialects.
 * @author Gavin King
 */

/* package-private */
abstract class AbstractTransactSQLDialect extends Dialect {
	public AbstractTransactSQLDialect() {
		super();
		registerColumnType( Types.BIT, "tinyint" ); //Sybase BIT type does not support null values
		registerColumnType( Types.BIGINT, "numeric(19,0)" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.INTEGER, "int" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "datetime" );
		registerColumnType( Types.TIME, "datetime" );
		registerColumnType( Types.TIMESTAMP, "datetime" );
		registerColumnType( Types.VARBINARY, "varbinary($l)" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.BLOB, "image" );
		registerColumnType( Types.CLOB, "text" );

		registerFunction( "ascii", new StandardSQLFunction("ascii", Hibernate.INTEGER) );
		registerFunction( "char", new StandardSQLFunction("char", Hibernate.CHARACTER) );
		registerFunction( "len", new StandardSQLFunction("len", Hibernate.LONG) );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "str", new StandardSQLFunction("str", Hibernate.STRING) );
		registerFunction( "ltrim", new StandardSQLFunction("ltrim") );
		registerFunction( "rtrim", new StandardSQLFunction("rtrim") );
		registerFunction( "reverse", new StandardSQLFunction("reverse") );
		registerFunction( "space", new StandardSQLFunction("space", Hibernate.STRING) );

		registerFunction( "user", new NoArgSQLFunction("user", Hibernate.STRING) );

		registerFunction( "current_timestamp", new NoArgSQLFunction("getdate", Hibernate.TIMESTAMP) );
		registerFunction( "current_time", new NoArgSQLFunction("getdate", Hibernate.TIME) );
		registerFunction( "current_date", new NoArgSQLFunction("getdate", Hibernate.DATE) );

		registerFunction( "getdate", new NoArgSQLFunction("getdate", Hibernate.TIMESTAMP) );
		registerFunction( "getutcdate", new NoArgSQLFunction("getutcdate", Hibernate.TIMESTAMP) );
		registerFunction( "day", new StandardSQLFunction("day", Hibernate.INTEGER) );
		registerFunction( "month", new StandardSQLFunction("month", Hibernate.INTEGER) );
		registerFunction( "year", new StandardSQLFunction("year", Hibernate.INTEGER) );
		registerFunction( "datename", new StandardSQLFunction("datename", Hibernate.STRING) );

		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction("sign", Hibernate.INTEGER) );

		registerFunction( "acos", new StandardSQLFunction("acos", Hibernate.DOUBLE) );
		registerFunction( "asin", new StandardSQLFunction("asin", Hibernate.DOUBLE) );
		registerFunction( "atan", new StandardSQLFunction("atan", Hibernate.DOUBLE) );
		registerFunction( "cos", new StandardSQLFunction("cos", Hibernate.DOUBLE) );
		registerFunction( "cot", new StandardSQLFunction("cot", Hibernate.DOUBLE) );
		registerFunction( "exp", new StandardSQLFunction("exp", Hibernate.DOUBLE) );
		registerFunction( "log", new StandardSQLFunction( "log", Hibernate.DOUBLE) );
		registerFunction( "log10", new StandardSQLFunction("log10", Hibernate.DOUBLE) );
		registerFunction( "sin", new StandardSQLFunction("sin", Hibernate.DOUBLE) );
		registerFunction( "sqrt", new StandardSQLFunction("sqrt", Hibernate.DOUBLE) );
		registerFunction( "tan", new StandardSQLFunction("tan", Hibernate.DOUBLE) );
		registerFunction( "pi", new NoArgSQLFunction("pi", Hibernate.DOUBLE) );
		registerFunction( "square", new StandardSQLFunction("square") );
		registerFunction( "rand", new StandardSQLFunction("rand", Hibernate.FLOAT) );

		registerFunction("radians", new StandardSQLFunction("radians", Hibernate.DOUBLE) );
		registerFunction("degrees", new StandardSQLFunction("degrees", Hibernate.DOUBLE) );

		registerFunction( "round", new StandardSQLFunction("round") );
		registerFunction( "ceiling", new StandardSQLFunction("ceiling") );
		registerFunction( "floor", new StandardSQLFunction("floor") );

		registerFunction( "isnull", new StandardSQLFunction("isnull") );

		registerFunction( "concat", new VarArgsSQLFunction( Hibernate.STRING, "(","+",")" ) );

		registerFunction( "length", new StandardSQLFunction( "len", Hibernate.INTEGER ) );
		registerFunction( "trim", new SQLFunctionTemplate( Hibernate.STRING, "ltrim(rtrim(?1))") );
		registerFunction( "locate", new CharIndexFunction() );

		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, NO_BATCH);
	}

	public String getAddColumnString() {
		return "add";
	}
	public String getNullColumnString() {
		return " null";
	}
	public boolean qualifyIndexName() {
		return false;
	}

	public String getForUpdateString() {
		return "";
	}

	public boolean supportsIdentityColumns() {
		return true;
	}
	public String getIdentitySelectString() {
		return "select @@identity";
	}
	public String getIdentityColumnString() {
		return "identity not null"; //starts with 1, implicitly
	}

	public boolean supportsInsertSelectIdentity() {
		return true;
	}

	public String appendIdentitySelectToInsert(String insertSQL) {
		return insertSQL + "\nselect @@identity";
	}

	public String appendLockHint(LockMode mode, String tableName) {
		if ( mode.greaterThan( LockMode.READ ) ) {
			return tableName + " holdlock";
		}
		else {
			return tableName;
		}
	}

	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map keyColumnNames) {
		// TODO:  merge additional lockoptions support in Dialect.applyLocksToSql 
		Iterator itr = aliasedLockOptions.getAliasLockIterator();
		StringBuffer buffer = new StringBuffer( sql );
		int correction = 0;
		while ( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			final LockMode lockMode = ( LockMode ) entry.getValue();
			if ( lockMode.greaterThan( LockMode.READ ) ) {
				final String alias = ( String ) entry.getKey();
				int start = -1, end = -1;
				if ( sql.endsWith( " " + alias ) ) {
					start = ( sql.length() - alias.length() ) + correction;
					end = start + alias.length();
				}
				else {
					int position = sql.indexOf( " " + alias + " " );
					if ( position <= -1 ) {
						position = sql.indexOf( " " + alias + "," );
					}
					if ( position > -1 ) {
						start = position + correction + 1;
						end = start + alias.length();
					}
				}

				if ( start > -1 ) {
					final String lockHint = appendLockHint( lockMode, alias );
					buffer.replace( start, end, lockHint );
					correction += ( lockHint.length() - alias.length() );
				}
			}
		}
		return buffer.toString();
	}

	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col; // sql server just returns automatically
	}

	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
//		 This assumes you will want to ignore any update counts
		while ( !isResultSet && ps.getUpdateCount() != -1 ) {
			isResultSet = ps.getMoreResults();
		}
//		 You may still have other ResultSets or update counts left to process here
//		 but you can't do it now or the ResultSet you just got will be closed
		return ps.getResultSet();
	}

	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	public String getCurrentTimestampSelectString() {
		return "select getdate()";
	}

	public boolean supportsTemporaryTables() {
		return true;
	}

	public String generateTemporaryTableName(String baseTableName) {
		return "#" + baseTableName;
	}

	public boolean dropTemporaryTableAfterUse() {
		return true;  // sql-server, at least needed this dropped after use; strange!
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean supportsEmptyInList() {
		return false;
	}

	public boolean supportsExistsInSelect() {
		return false;
	}

	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return true;
	}
}
