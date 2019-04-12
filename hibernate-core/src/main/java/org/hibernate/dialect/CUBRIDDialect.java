/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.dialect.identity.CUBRIDIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.CUBRIDLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorCUBRIDDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An SQL dialect for CUBRID (8.3.x and later).
 *
 * @author Seok Jeong Il
 */
public class CUBRIDDialect extends Dialect {
	/**
	 * Constructs a CUBRIDDialect
	 */
	public CUBRIDDialect() {
		super();

		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.BIT, "bit(8)" );
		registerColumnType( Types.BLOB, "bit varying(65535)" );
		registerColumnType( Types.BOOLEAN, "bit(8)" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.CLOB, "string" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.DECIMAL, "decimal" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.INTEGER, "int" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.REAL, "double" );
		registerColumnType( Types.SMALLINT, "short" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TINYINT, "short" );
		registerColumnType( Types.VARBINARY, 2000, "bit varying($l)" );
		registerColumnType( Types.VARCHAR, "string" );
		registerColumnType( Types.VARCHAR, 2000, "varchar($l)" );
		registerColumnType( Types.VARCHAR, 255, "varchar($l)" );

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

		registerKeyword( "TYPE" );
		registerKeyword( "YEAR" );
		registerKeyword( "MONTH" );
		registerKeyword( "ALIAS" );
		registerKeyword( "VALUE" );
		registerKeyword( "FIRST" );
		registerKeyword( "ROLE" );
		registerKeyword( "CLASS" );
		registerKeyword( "BIT" );
		registerKeyword( "TIME" );
		registerKeyword( "QUERY" );
		registerKeyword( "DATE" );
		registerKeyword( "USER" );
		registerKeyword( "ACTION" );
		registerKeyword( "SYS_USER" );
		registerKeyword( "ZONE" );
		registerKeyword( "LANGUAGE" );
		registerKeyword( "DICTIONARY" );
		registerKeyword( "DATA" );
		registerKeyword( "TEST" );
		registerKeyword( "SUPERCLASS" );
		registerKeyword( "SECTION" );
		registerKeyword( "LOWER" );
		registerKeyword( "LIST" );
		registerKeyword( "OID" );
		registerKeyword( "DAY" );
		registerKeyword( "IF" );
		registerKeyword( "ATTRIBUTE" );
		registerKeyword( "STRING" );
		registerKeyword( "SEARCH" );
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		queryEngine.getSqmFunctionRegistry().registerNamed( "ascii", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "bin", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "char_length", StandardSpiBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNamed( "character_length", StandardSpiBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNamed( "lengthb", StandardSpiBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNamed( "lengthh", StandardSpiBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNamed( "lcase" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "lower" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "ltrim" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "reverse" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "rtrim" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "trim" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "space", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "ucase" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "upper" );

		queryEngine.getSqmFunctionRegistry().registerNamed( "abs" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "sign", StandardSpiBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().registerNamed( "acos", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "asin", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "atan", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "cos", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "cot", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "exp", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "ln", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "log2", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "log10", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "pi", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "rand", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "random", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "sin", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "sqrt", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "tan", StandardSpiBasicTypes.DOUBLE );

		queryEngine.getSqmFunctionRegistry().registerNamed( "radians", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "degrees", StandardSpiBasicTypes.DOUBLE );

		queryEngine.getSqmFunctionRegistry().registerNamed( "ceil", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "floor", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "round" );

		queryEngine.getSqmFunctionRegistry().registerNamed( "datediff", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "timediff", StandardSpiBasicTypes.TIME );

		queryEngine.getSqmFunctionRegistry().registerNamed( "date", StandardSpiBasicTypes.DATE );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "curdate", StandardSpiBasicTypes.DATE );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "current_date", StandardSpiBasicTypes.DATE );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "sys_date", StandardSpiBasicTypes.DATE );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "sysdate", StandardSpiBasicTypes.DATE );

		queryEngine.getSqmFunctionRegistry().registerNamed( "time", StandardSpiBasicTypes.TIME );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "curtime", StandardSpiBasicTypes.TIME );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "current_time", StandardSpiBasicTypes.TIME );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "sys_time", StandardSpiBasicTypes.TIME );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "systime", StandardSpiBasicTypes.TIME );

		queryEngine.getSqmFunctionRegistry().registerNamed( "timestamp", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "current_timestamp", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "sys_timestamp", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "systimestamp", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "localtime", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "localtimestamp", StandardSpiBasicTypes.TIMESTAMP );

		queryEngine.getSqmFunctionRegistry().registerNamed( "day", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayofmonth", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayofweek", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayofyear", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "from_days", StandardSpiBasicTypes.DATE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "from_unixtime", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNamed( "last_day", StandardSpiBasicTypes.DATE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "minute", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "month", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "months_between", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "now", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNamed( "quarter", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "second", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "sec_to_time", StandardSpiBasicTypes.TIME );
		queryEngine.getSqmFunctionRegistry().registerNamed( "time_to_sec", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "to_days", StandardSpiBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNamed( "unix_timestamp", StandardSpiBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "utc_date", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "utc_time", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "week", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "weekday", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "year", StandardSpiBasicTypes.INTEGER );

		queryEngine.getSqmFunctionRegistry().registerNamed( "hex", StandardSpiBasicTypes.STRING );

		queryEngine.getSqmFunctionRegistry().registerNamed( "octet_length", StandardSpiBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNamed( "bit_length", StandardSpiBasicTypes.LONG );

		queryEngine.getSqmFunctionRegistry().registerNamed( "bit_count", StandardSpiBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNamed( "md5", StandardSpiBasicTypes.STRING );

		queryEngine.getSqmFunctionRegistry().registerNamed( "concat", StandardSpiBasicTypes.STRING );

		queryEngine.getSqmFunctionRegistry().registerNamed( "substring", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "substr", StandardSpiBasicTypes.STRING );

		queryEngine.getSqmFunctionRegistry().registerNamed( "length", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "bit_length", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "coalesce" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "nullif" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "mod" );

		queryEngine.getSqmFunctionRegistry().registerNamed( "power" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "stddev" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "variance" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "trunc" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "nvl" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "nvl2" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "chr", StandardSpiBasicTypes.CHARACTER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "to_char", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "to_date", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNamed( "instr", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "instrb", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "lpad", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "replace", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "rpad", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "translate", StandardSpiBasicTypes.STRING );

		queryEngine.getSqmFunctionRegistry().registerNamed( "add_months", StandardSpiBasicTypes.DATE );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "user", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "rownum", StandardSpiBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerVarArgs( "concat", StandardSpiBasicTypes.STRING, "", "||", "" );
	}

	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + sequenceName + ".next_value from table({1}) as T(X)";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create serial " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop serial " + sequenceName;
	}

	@Override
	public String getDropForeignKeyString() {
		return " drop foreign key ";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from db_serial";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorCUBRIDDatabaseImpl.INSTANCE;
	}

	@Override
	public char openQuote() {
		return '[';
	}

	@Override
	public char closeQuote() {
		return ']';
	}

	@Override
	public String getForUpdateString() {
		return " ";
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select now()";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return CUBRIDLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new CUBRIDIdentityColumnSupport();
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}
}
