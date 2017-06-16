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

import org.hibernate.JDBCException;
import org.hibernate.NullPrecedence;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgsSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.MySQLIdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.AfterUseAction;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An SQL dialect for MySQL (prior to 5.x).
 *
 * @author Gavin King
 */
@SuppressWarnings("deprecation")
public class MySQLDialect extends Dialect {

	private static final LimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			return sql + (hasOffset ? " limit ?, ?" : " limit ?");
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}
	};

	/**
	 * Constructs a MySQLDialect
	 */
	public MySQLDialect() {
		super();
		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.BOOLEAN, "bit" ); // HHH-6935
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "datetime" );
		registerColumnType( Types.VARBINARY, "longblob" );
		registerColumnType( Types.VARBINARY, 16777215, "mediumblob" );
		registerColumnType( Types.VARBINARY, 65535, "blob" );
		registerColumnType( Types.VARBINARY, 255, "tinyblob" );
		registerColumnType( Types.BINARY, "binary($l)" );
		registerColumnType( Types.LONGVARBINARY, "longblob" );
		registerColumnType( Types.LONGVARBINARY, 16777215, "mediumblob" );
		registerColumnType( Types.NUMERIC, "decimal($p,$s)" );
		registerColumnType( Types.BLOB, "longblob" );
//		registerColumnType( Types.BLOB, 16777215, "mediumblob" );
//		registerColumnType( Types.BLOB, 65535, "blob" );
		registerColumnType( Types.CLOB, "longtext" );
		registerColumnType( Types.NCLOB, "longtext" );
//		registerColumnType( Types.CLOB, 16777215, "mediumtext" );
//		registerColumnType( Types.CLOB, 65535, "text" );
		registerVarcharTypes();

		registerFunction( "ascii", new NamedSqmFunctionTemplate( "ascii", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "bin", new NamedSqmFunctionTemplate( "bin", StandardSpiBasicTypes.STRING ) );
		registerFunction( "char_length", new NamedSqmFunctionTemplate( "char_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "character_length", new NamedSqmFunctionTemplate( "character_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "lcase", new NamedSqmFunctionTemplate( "lcase" ) );
		registerFunction( "lower", new NamedSqmFunctionTemplate( "lower" ) );
		registerFunction( "ltrim", new NamedSqmFunctionTemplate( "ltrim" ) );
		registerFunction( "ord", new NamedSqmFunctionTemplate( "ord", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "quote", new NamedSqmFunctionTemplate( "quote" ) );
		registerFunction( "reverse", new NamedSqmFunctionTemplate( "reverse" ) );
		registerFunction( "rtrim", new NamedSqmFunctionTemplate( "rtrim" ) );
		registerFunction( "soundex", new NamedSqmFunctionTemplate( "soundex" ) );
		registerFunction( "space", new NamedSqmFunctionTemplate( "space", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ucase", new NamedSqmFunctionTemplate( "ucase" ) );
		registerFunction( "upper", new NamedSqmFunctionTemplate( "upper" ) );
		registerFunction( "unhex", new NamedSqmFunctionTemplate( "unhex", StandardSpiBasicTypes.STRING ) );

		registerFunction( "abs", new NamedSqmFunctionTemplate( "abs" ) );
		registerFunction( "sign", new NamedSqmFunctionTemplate( "sign", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "acos", new NamedSqmFunctionTemplate( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new NamedSqmFunctionTemplate( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new NamedSqmFunctionTemplate( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new NamedSqmFunctionTemplate( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cot", new NamedSqmFunctionTemplate( "cot", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "crc32", new NamedSqmFunctionTemplate( "crc32", StandardSpiBasicTypes.LONG ) );
		registerFunction( "exp", new NamedSqmFunctionTemplate( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "ln", new NamedSqmFunctionTemplate( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log", new NamedSqmFunctionTemplate( "log", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log2", new NamedSqmFunctionTemplate( "log2", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log10", new NamedSqmFunctionTemplate( "log10", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgsSqmFunctionTemplate( "pi", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "rand", new NoArgsSqmFunctionTemplate( "rand", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sin", new NamedSqmFunctionTemplate( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new NamedSqmFunctionTemplate( "sqrt", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "stddev", new NamedSqmFunctionTemplate( "std", StandardSpiBasicTypes.DOUBLE) );
		registerFunction( "tan", new NamedSqmFunctionTemplate( "tan", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "radians", new NamedSqmFunctionTemplate( "radians", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new NamedSqmFunctionTemplate( "degrees", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "ceiling", new NamedSqmFunctionTemplate( "ceiling", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "ceil", new NamedSqmFunctionTemplate( "ceil", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "floor", new NamedSqmFunctionTemplate( "floor", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "round", new NamedSqmFunctionTemplate( "round" ) );

		registerFunction( "datediff", new NamedSqmFunctionTemplate( "datediff", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "timediff", new NamedSqmFunctionTemplate( "timediff", StandardSpiBasicTypes.TIME ) );
		registerFunction( "date_format", new NamedSqmFunctionTemplate( "date_format", StandardSpiBasicTypes.STRING ) );

		registerFunction( "curdate", new NoArgsSqmFunctionTemplate( "curdate", StandardSpiBasicTypes.DATE ) );
		registerFunction( "curtime", new NoArgsSqmFunctionTemplate( "curtime", StandardSpiBasicTypes.TIME ) );
		registerFunction( "current_date", new NoArgsSqmFunctionTemplate( "current_date", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "current_time", new NoArgsSqmFunctionTemplate( "current_time", StandardSpiBasicTypes.TIME, false ) );
		registerFunction( "current_timestamp", new NoArgsSqmFunctionTemplate( "current_timestamp", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction( "date", new NamedSqmFunctionTemplate( "date", StandardSpiBasicTypes.DATE ) );
		registerFunction( "day", new NamedSqmFunctionTemplate( "day", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofmonth", new NamedSqmFunctionTemplate( "dayofmonth", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayname", new NamedSqmFunctionTemplate( "dayname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "dayofweek", new NamedSqmFunctionTemplate( "dayofweek", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new NamedSqmFunctionTemplate( "dayofyear", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "from_days", new NamedSqmFunctionTemplate( "from_days", StandardSpiBasicTypes.DATE ) );
		registerFunction( "from_unixtime", new NamedSqmFunctionTemplate( "from_unixtime", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "hour", new NamedSqmFunctionTemplate( "hour", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "last_day", new NamedSqmFunctionTemplate( "last_day", StandardSpiBasicTypes.DATE ) );
		registerFunction( "localtime", new NoArgsSqmFunctionTemplate( "localtime", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "localtimestamp", new NoArgsSqmFunctionTemplate( "localtimestamp", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "microseconds", new NamedSqmFunctionTemplate( "microseconds", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "minute", new NamedSqmFunctionTemplate( "minute", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "month", new NamedSqmFunctionTemplate( "month", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "monthname", new NamedSqmFunctionTemplate( "monthname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "now", new NoArgsSqmFunctionTemplate( "now", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "quarter", new NamedSqmFunctionTemplate( "quarter", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "second", new NamedSqmFunctionTemplate( "second", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "sec_to_time", new NamedSqmFunctionTemplate( "sec_to_time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "sysdate", new NoArgsSqmFunctionTemplate( "sysdate", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "time", new NamedSqmFunctionTemplate( "time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "timestamp", new NamedSqmFunctionTemplate( "timestamp", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "time_to_sec", new NamedSqmFunctionTemplate( "time_to_sec", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "to_days", new NamedSqmFunctionTemplate( "to_days", StandardSpiBasicTypes.LONG ) );
		registerFunction( "unix_timestamp", new NamedSqmFunctionTemplate( "unix_timestamp", StandardSpiBasicTypes.LONG ) );
		registerFunction( "utc_date", new NoArgsSqmFunctionTemplate( "utc_date", StandardSpiBasicTypes.STRING ) );
		registerFunction( "utc_time", new NoArgsSqmFunctionTemplate( "utc_time", StandardSpiBasicTypes.STRING ) );
		registerFunction( "utc_timestamp", new NoArgsSqmFunctionTemplate( "utc_timestamp", StandardSpiBasicTypes.STRING ) );
		registerFunction( "week", new NamedSqmFunctionTemplate( "week", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "weekday", new NamedSqmFunctionTemplate( "weekday", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "weekofyear", new NamedSqmFunctionTemplate( "weekofyear", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "year", new NamedSqmFunctionTemplate( "year", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "yearweek", new NamedSqmFunctionTemplate( "yearweek", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "hex", new NamedSqmFunctionTemplate( "hex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "oct", new NamedSqmFunctionTemplate( "oct", StandardSpiBasicTypes.STRING ) );

		registerFunction( "octet_length", new NamedSqmFunctionTemplate( "octet_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "bit_length", new NamedSqmFunctionTemplate( "bit_length", StandardSpiBasicTypes.LONG ) );

		registerFunction( "bit_count", new NamedSqmFunctionTemplate( "bit_count", StandardSpiBasicTypes.LONG ) );
		registerFunction( "encrypt", new NamedSqmFunctionTemplate( "encrypt", StandardSpiBasicTypes.STRING ) );
		registerFunction( "md5", new NamedSqmFunctionTemplate( "md5", StandardSpiBasicTypes.STRING ) );
		registerFunction( "sha1", new NamedSqmFunctionTemplate( "sha1", StandardSpiBasicTypes.STRING ) );
		registerFunction( "sha", new NamedSqmFunctionTemplate( "sha", StandardSpiBasicTypes.STRING ) );

		registerFunction( "concat", new NamedSqmFunctionTemplate( "concat", StandardSpiBasicTypes.STRING ) );

		getDefaultProperties().setProperty( Environment.MAX_FETCH_DEPTH, "2" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
	}

	protected void registerVarcharTypes() {
		registerColumnType( Types.VARCHAR, "longtext" );
//		registerColumnType( Types.VARCHAR, 16777215, "mediumtext" );
//		registerColumnType( Types.VARCHAR, 65535, "text" );
		registerColumnType( Types.VARCHAR, 255, "varchar($l)" );
		registerColumnType( Types.LONGVARCHAR, "longtext" );
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final String cols = StringHelper.join( ", ", foreignKey );
		final String referencedCols = StringHelper.join( ", ", primaryKey );
		return String.format(
				" add constraint %s foreign key (%s) references %s (%s)",
				constraintName,
				cols,
				referencedTable,
				referencedCols
		);
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public String getDropForeignKeyString() {
		return " drop foreign key ";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public String getLimitString(String sql, boolean hasOffset) {
		return sql + (hasOffset ? " limit ?, ?" : " limit ?");
	}

	@Override
	public char closeQuote() {
		return '`';
	}

	@Override
	public char openQuote() {
		return '`';
	}

	@Override
	public boolean canCreateCatalog() {
		return true;
	}

	@Override
	public String[] getCreateCatalogCommand(String catalogName) {
		return new String[] { "create database " + catalogName };
	}

	@Override
	public String[] getDropCatalogCommand(String catalogName) {
		return new String[] { "drop database " + catalogName };
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public String[] getCreateSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "MySQL does not support dropping creating/dropping schemas in the JDBC sense" );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "MySQL does not support dropping creating/dropping schemas in the JDBC sense" );
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public String getSelectGUIDString() {
		return "select uuid()";
	}

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public String getTableComment(String comment) {
		return " comment='" + comment + "'";
	}

	@Override
	public String getColumnComment(String comment) {
		return " comment '" + comment + "'";
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new LocalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String getCreateIdTableCommand() {
						return "create temporary table if not exists";
					}

					@Override
					public String getDropIdTableCommand() {
						return "drop temporary table";
					}
				},
				AfterUseAction.DROP,
				TempTableDdlTransactionHandling.NONE
		);
	}

	@Override
	public String getCastTypeName(int code) {
		switch ( code ) {
			case Types.BOOLEAN:
				return "char";
			case Types.INTEGER:
			case Types.BIGINT:
			case Types.SMALLINT:
				return smallIntegerCastTarget();
			case Types.FLOAT:
			case Types.REAL: {
				return floatingPointNumberCastTarget();
			}
			case Types.NUMERIC:
				return fixedPointNumberCastTarget();
			case Types.VARCHAR:
				return "char";
			case Types.VARBINARY:
				return "binary";
			default:
				return super.getCastTypeName( code );
		}
	}

	/**
	 * Determine the cast target for {@link Types#INTEGER}, {@link Types#BIGINT} and {@link Types#SMALLINT}
	 *
	 * @return The proper cast target type.
	 */
	protected String smallIntegerCastTarget() {
		return "signed";
	}

	/**
	 * Determine the cast target for {@link Types#FLOAT} and {@link Types#REAL} (DOUBLE)
	 *
	 * @return The proper cast target type.
	 */
	protected String floatingPointNumberCastTarget() {
		// MySQL does not allow casting to DOUBLE nor FLOAT, so we have to cast these as DECIMAL.
		// MariaDB does allow casting to DOUBLE, although not FLOAT.
		return fixedPointNumberCastTarget();
	}

	/**
	 * Determine the cast target for {@link Types#NUMERIC}
	 *
	 * @return The proper cast target type.
	 */
	protected String fixedPointNumberCastTarget() {
		// NOTE : the precision/scale are somewhat arbitrary choices, but MySQL/MariaDB
		// effectively require *some* values
		return "decimal(" + Column.DEFAULT_PRECISION + "," + Column.DEFAULT_SCALE + ")";
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
		return "select now()";
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
		while ( !isResultSet && ps.getUpdateCount() != -1 ) {
			isResultSet = ps.getMoreResults();
		}
		return ps.getResultSet();
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return true;
	}

	@Override
	public String renderOrderByElement(String expression, String collation, String order, NullPrecedence nulls) {
		final StringBuilder orderByElement = new StringBuilder();
		if ( nulls != NullPrecedence.NONE ) {
			// Workaround for NULLS FIRST / LAST support.
			orderByElement.append( "case when " ).append( expression ).append( " is null then " );
			if ( nulls == NullPrecedence.FIRST ) {
				orderByElement.append( "0 else 1" );
			}
			else {
				orderByElement.append( "1 else 0" );
			}
			orderByElement.append( " end, " );
		}
		// Nulls precedence has already been handled so passing NONE value.
		orderByElement.append( super.renderOrderByElement( expression, collation, order, NullPrecedence.NONE ) );
		return orderByElement.toString();
	}

	// locking support

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public String getWriteLockString(int timeout) {
		return " for update";
	}

	@Override
	public String getReadLockString(int timeout) {
		return " lock in share mode";
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		// note: at least my local MySQL 5.1 install shows this not working...
		return false;
	}

	@Override
	public boolean supportsSubqueryOnMutatingTable() {
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		// yes, we do handle "lock timeout" conditions in the exception conversion delegate,
		// but that's a hardcoded lock timeout period across the whole entire MySQL database.
		// MySQL does not support specifying lock timeouts as part of the SQL statement, which is really
		// what this meta method is asking.
		return false;
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );

				if ( "41000".equals( sqlState ) ) {
					return new LockTimeoutException( message, sqlException, sql );
				}

				if ( "40001".equals( sqlState ) ) {
					return new LockAcquisitionException( message, sqlException, sql );
				}

				return null;
			}
		};
	}

	@Override
	public String getNotExpression(String expression) {
		return "not (" + expression + ")";
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new MySQLIdentityColumnSupport();
	}

	@Override
	public boolean isJdbcLogWarningsEnabledByDefault() {
		return false;
	}
}
