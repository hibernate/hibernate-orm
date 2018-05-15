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
import org.hibernate.PessimisticLockException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.MySQLIdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.dialect.unique.MySQLUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.model.relational.spi.Size;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTable;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTempTableExporter;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.spi.ConcatFunctionTemplate;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An SQL dialect for MySQL (prior to 5.x).
 *
 * @author Gavin King
 */
@SuppressWarnings("deprecation")
public class MySQLDialect extends Dialect {

	private final UniqueDelegate uniqueDelegate;
	private MySQLStorageEngine storageEngine;

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

		String storageEngine = Environment.getProperties().getProperty( Environment.STORAGE_ENGINE );
		if(storageEngine == null) {
			storageEngine = System.getProperty( Environment.STORAGE_ENGINE );
		}
		if(storageEngine == null) {
			this.storageEngine = getDefaultMySQLStorageEngine();
		}
		else if( "innodb".equals( storageEngine.toLowerCase() ) ) {
			this.storageEngine = InnoDBStorageEngine.INSTANCE;
		}
		else if( "myisam".equals( storageEngine.toLowerCase() ) ) {
			this.storageEngine = MyISAMStorageEngine.INSTANCE;
		}
		else {
			throw new UnsupportedOperationException( "The " + storageEngine + " storage engine is not supported!" );
		}

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

		getDefaultProperties().setProperty( Environment.MAX_FETCH_DEPTH, "2" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

		uniqueDelegate = new MySQLUniqueDelegate( this );
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		registry.namedTemplateBuilder( "ascii" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "bin" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "char_length" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		registry.registerAlternateKey( "character_length", "char_length" );
		registry.register( "concat", new ConcatFunctionTemplate("concat(", ", ", ")"));
		CommonFunctionFactory.lower( registry );
		registry.registerAlternateKey( "lcase", "lower" );
		registry.namedTemplateBuilder( "ltrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "ord" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "quote" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "reverse" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "rtrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();

		CommonFunctionFactory.soundex( registry );

		registry.namedTemplateBuilder( "space" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.upper( registry );
		registry.registerAlternateKey( "ucase", "upper" );
		registry.namedTemplateBuilder( "unhex" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();

		CommonFunctionFactory.abs( registry );
		CommonFunctionFactory.sign( registry );
		CommonFunctionFactory.acos( registry );
		CommonFunctionFactory.asin( registry );
		CommonFunctionFactory.atan( registry );
		CommonFunctionFactory.atan( registry );
		CommonFunctionFactory.cos( registry );
		CommonFunctionFactory.cot( registry );
		registry.namedTemplateBuilder( "crc32" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.exp( registry );
		CommonFunctionFactory.ln( registry );
		CommonFunctionFactory.log( registry );
		registry.namedTemplateBuilder( "log2" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.log10( registry );
		registry.registerNoArgs( "pi", StandardSpiBasicTypes.DOUBLE );
		registry.registerNoArgs( "rand", StandardSpiBasicTypes.DOUBLE );
		CommonFunctionFactory.sin( registry );
		CommonFunctionFactory.sqrt( registry );
		CommonFunctionFactory.tan( registry );
		registry.namedTemplateBuilder( "stddev", "std" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.radians( registry );
		CommonFunctionFactory.degrees( registry );

		CommonFunctionFactory.ceiling( registry );
		CommonFunctionFactory.ceil( registry );
		CommonFunctionFactory.floor( registry );
		CommonFunctionFactory.round( registry );

		registry.namedTemplateBuilder( "datediff" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.register();
		registry.namedTemplateBuilder( "timediff" )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setExactArgumentCount( 2 )
				.register();
		registry.namedTemplateBuilder( "date_format" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();

		registry.registerNoArgs( "curdate", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "curtime", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "current_date", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "current_time", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "current_timestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.namedTemplateBuilder( "date" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "day" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "dayofmonth" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "dayname" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "dayofweek" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "dayofyear" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "from_days" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "from_unixtime" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "hour" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "last_day" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 1 )
				.register();
		registry.registerNoArgs( "localtime", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "localtimestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.namedTemplateBuilder( "microseconds" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "minute" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "month" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "monthname" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.registerNoArgs( "now", StandardSpiBasicTypes.TIMESTAMP );
		registry.namedTemplateBuilder( "quarter" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "second" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "sec_to_time" )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setExactArgumentCount( 1 )
				.register();
		registry.registerNoArgs( "sysdate", StandardSpiBasicTypes.TIMESTAMP );
		registry.namedTemplateBuilder( "time" )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "timestamp" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.namedTemplateBuilder( "time_to_sec" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "to_days" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "unix_timestamp" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setArgumentCountBetween( 0, 1 )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		registry.registerNoArgs( "utc_date", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "utc_time", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "utc_timestamp", StandardSpiBasicTypes.STRING );
		registry.namedTemplateBuilder( "week" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.namedTemplateBuilder( "weekday" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "weekofyear" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "year" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "yearweek" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween( 1, 2 )
				.register();

		registry.namedTemplateBuilder( "hex" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "oct" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();

		registry.namedTemplateBuilder( "octet_length" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "bit_length" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();

		registry.namedTemplateBuilder( "bit_count" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "encrypt" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.namedTemplateBuilder( "md5" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "sha1" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "sha" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
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
		final String cols = String.join( ", ", foreignKey );
		final String referencedCols = String.join( ", ", primaryKey );
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
	public String getTableComment(String comment) {
		return " comment='" + comment + "'";
	}

	@Override
	public String getColumnComment(String comment) {
		return " comment '" + comment + "'";
	}

	@Override
	public IdTableStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( generateIdTableExporter() );
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new LocalTempTableExporter() {
			@Override
			public String getCreateCommand() {
				return "create temporary table if not exists";
			}

			@Override
			public String getDropCommand() {
				return "drop temporary table";
			}
		};
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
		return "decimal(" + Size.Builder.DEFAULT_PRECISION + "," + Size.Builder.DEFAULT_SCALE + ")";
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
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
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
				switch ( sqlException.getErrorCode() ) {
					case 1205: {
						return new PessimisticLockException( message, sqlException, sql );
					}
					case 1207:
					case 1206: {
						return new LockAcquisitionException( message, sqlException, sql );
					}
				}

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

	@Override
	public boolean supportsCascadeDelete() {
		return storageEngine.supportsCascadeDelete();
	}

	@Override
	public String getTableTypeString() {
		return storageEngine.getTableTypeString( getEngineKeyword());
	}

	protected String getEngineKeyword() {
		return "type";
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return storageEngine.hasSelfReferentialForeignKeyBug();
	}

	@Override
	public boolean dropConstraints() {
		return storageEngine.dropConstraints();
	}

	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return MyISAMStorageEngine.INSTANCE;
	}

	@Override
	protected String escapeLiteral(String literal) {
		return super.escapeLiteral( literal ).replace("\\", "\\\\");
	}
}
