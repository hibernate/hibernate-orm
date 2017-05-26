/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.cfg.Environment;
import org.hibernate.query.sqm.produce.function.spi.CharIndexFunctionTemplate;
import org.hibernate.dialect.function.NoArgsSqmFunctionTemplate;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.StandardSqmFunctionTemplate;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.identity.AbstractTransactSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An abstract base class for Sybase and MS SQL Server dialects.
 *
 * @author Gavin King
 */
abstract class AbstractTransactSQLDialect extends Dialect {
	public AbstractTransactSQLDialect() {
		super();
		registerColumnType( Types.BINARY, "binary($l)" );
		registerColumnType( Types.BIT, "tinyint" );
		registerColumnType( Types.BIGINT, "numeric(19,0)" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" );
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

		registerFunction( "ascii", new StandardSqmFunctionTemplate( "ascii", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "char", new StandardSqmFunctionTemplate( "char", StandardSpiBasicTypes.CHARACTER ) );
		registerFunction( "len", new StandardSqmFunctionTemplate( "len", StandardSpiBasicTypes.LONG ) );
		registerFunction( "lower", new StandardSqmFunctionTemplate( "lower" ) );
		registerFunction( "upper", new StandardSqmFunctionTemplate( "upper" ) );
		registerFunction( "str", new StandardSqmFunctionTemplate( "str", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ltrim", new StandardSqmFunctionTemplate( "ltrim" ) );
		registerFunction( "rtrim", new StandardSqmFunctionTemplate( "rtrim" ) );
		registerFunction( "reverse", new StandardSqmFunctionTemplate( "reverse" ) );
		registerFunction( "space", new StandardSqmFunctionTemplate( "space", StandardSpiBasicTypes.STRING ) );

		registerFunction( "user", new NoArgsSqmFunctionTemplate( "user", StandardSpiBasicTypes.STRING ) );

		registerFunction( "current_timestamp", new NoArgsSqmFunctionTemplate( "getdate", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "current_time", new NoArgsSqmFunctionTemplate( "getdate", StandardSpiBasicTypes.TIME ) );
		registerFunction( "current_date", new NoArgsSqmFunctionTemplate( "getdate", StandardSpiBasicTypes.DATE ) );

		registerFunction( "getdate", new NoArgsSqmFunctionTemplate( "getdate", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "getutcdate", new NoArgsSqmFunctionTemplate( "getutcdate", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "day", new StandardSqmFunctionTemplate( "day", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSqmFunctionTemplate( "month", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "year", new StandardSqmFunctionTemplate( "year", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "datename", new StandardSqmFunctionTemplate( "datename", StandardSpiBasicTypes.STRING ) );

		registerFunction( "abs", new StandardSqmFunctionTemplate( "abs" ) );
		registerFunction( "sign", new StandardSqmFunctionTemplate( "sign", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "acos", new StandardSqmFunctionTemplate( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSqmFunctionTemplate( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSqmFunctionTemplate( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSqmFunctionTemplate( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSqmFunctionTemplate( "cot", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "exp", new StandardSqmFunctionTemplate( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSqmFunctionTemplate( "log", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardSqmFunctionTemplate( "log10", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSqmFunctionTemplate( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new StandardSqmFunctionTemplate( "sqrt", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSqmFunctionTemplate( "tan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgsSqmFunctionTemplate( "pi", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "square", new StandardSqmFunctionTemplate( "square" ) );
		registerFunction( "rand", new StandardSqmFunctionTemplate( "rand", StandardSpiBasicTypes.FLOAT ) );

		registerFunction( "radians", new StandardSqmFunctionTemplate( "radians", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new StandardSqmFunctionTemplate( "degrees", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "round", new StandardSqmFunctionTemplate( "round" ) );
		registerFunction( "ceiling", new StandardSqmFunctionTemplate( "ceiling" ) );
		registerFunction( "floor", new StandardSqmFunctionTemplate( "floor" ) );

		registerFunction( "isnull", new StandardSqmFunctionTemplate( "isnull" ) );

		registerFunction( "concat", new VarArgsSQLFunction( StandardSpiBasicTypes.STRING, "(", "+", ")" ) );

		registerFunction( "length", new StandardSqmFunctionTemplate( "len", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "trim", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "ltrim(rtrim(?1))" ) );
		registerFunction( "locate", new CharIndexFunctionTemplate() );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getNullColumnString() {
		return "";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public String appendLockHint(LockOptions lockOptions, String tableName) {
		return lockOptions.getLockMode().greaterThan( LockMode.READ ) ? tableName + " holdlock" : tableName;
	}

	@Override
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		// TODO:  merge additional lockoptions support in Dialect.applyLocksToSql
		final Iterator itr = aliasedLockOptions.getAliasLockIterator();
		final StringBuilder buffer = new StringBuilder( sql );
		int correction = 0;
		while ( itr.hasNext() ) {
			final Map.Entry entry = (Map.Entry) itr.next();
			final LockMode lockMode = (LockMode) entry.getValue();
			if ( lockMode.greaterThan( LockMode.READ ) ) {
				final String alias = (String) entry.getKey();
				int start = -1;
				int end = -1;
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
					final String lockHint = appendLockHint( aliasedLockOptions, alias );
					buffer.replace( start, end, lockHint );
					correction += ( lockHint.length() - alias.length() );
				}
			}
		}
		return buffer.toString();
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		// sql server just returns automatically
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
		// This assumes you will want to ignore any update counts
		while ( !isResultSet && ps.getUpdateCount() != -1 ) {
			isResultSet = ps.getMoreResults();
		}

		// You may still have other ResultSets or update counts left to process here
		// but you can't do it now or the ResultSet you just got will be closed
		return ps.getResultSet();
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select getdate()";
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new LocalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String generateIdTableName(String baseName) {
						return "#" + baseName;
					}
				},
				// sql-server, at least needed this dropped afterQuery use; strange!
				AfterUseAction.DROP,
				TempTableDdlTransactionHandling.NONE
		);
	}

	@Override
	public String getSelectGUIDString() {
		return "select newid()";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}
	
	@Override
	public boolean supportsTuplesInSubqueries() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new AbstractTransactSQLIdentityColumnSupport();
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}
}
