/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
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
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		registry.registerNamed( "ascii", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "bin", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "char_length", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "character_length", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "lengthb", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "lengthh", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "lcase" );
		registry.registerNamed( "lower" );
		registry.registerNamed( "ltrim" );
		registry.registerNamed( "reverse" );
		registry.registerNamed( "rtrim" );
		registry.registerNamed( "trim" );
		registry.registerNamed( "space", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "ucase" );
		registry.registerNamed( "upper" );

		registry.registerNamed( "abs" );
		registry.registerNamed( "sign", StandardSpiBasicTypes.INTEGER );

		registry.registerNamed( "acos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "asin", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "atan", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "cos", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "cot", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "exp", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "ln", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "log2", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "log10", StandardSpiBasicTypes.DOUBLE );
		registry.registerNoArgs( "pi", StandardSpiBasicTypes.DOUBLE );
		registry.registerNoArgs( "rand", StandardSpiBasicTypes.DOUBLE );
		registry.registerNoArgs( "random", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "sin", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "sqrt", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "tan", StandardSpiBasicTypes.DOUBLE );

		registry.registerNamed( "radians", StandardSpiBasicTypes.DOUBLE );
		registry.registerNamed( "degrees", StandardSpiBasicTypes.DOUBLE );

		registry.registerNamed( "ceil", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "floor", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "round" );

		registry.registerNamed( "datediff", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "timediff", StandardSpiBasicTypes.TIME );

		registry.registerNamed( "date", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "curdate", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "current_date", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "sys_date", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "sysdate", StandardSpiBasicTypes.DATE );

		registry.registerNamed( "time", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "curtime", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "current_time", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "sys_time", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "systime", StandardSpiBasicTypes.TIME );

		registry.registerNamed( "timestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "current_timestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "sys_timestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "systimestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "localtime", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNoArgs( "localtimestamp", StandardSpiBasicTypes.TIMESTAMP );

		registry.registerNamed( "day", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "dayofmonth", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "dayofweek", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "dayofyear", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "from_days", StandardSpiBasicTypes.DATE );
		registry.registerNamed( "from_unixtime", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNamed( "last_day", StandardSpiBasicTypes.DATE );
		registry.registerNamed( "minute", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "month", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "months_between", StandardSpiBasicTypes.DOUBLE );
		registry.registerNoArgs( "now", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNamed( "quarter", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "second", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "sec_to_time", StandardSpiBasicTypes.TIME );
		registry.registerNamed( "time_to_sec", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "to_days", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "unix_timestamp", StandardSpiBasicTypes.LONG );
		registry.registerNoArgs( "utc_date", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "utc_time", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "week", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "weekday", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "year", StandardSpiBasicTypes.INTEGER );

		registry.registerNamed( "hex", StandardSpiBasicTypes.STRING );

		registry.registerNamed( "octet_length", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "bit_length", StandardSpiBasicTypes.LONG );

		registry.registerNamed( "bit_count", StandardSpiBasicTypes.LONG );
		registry.registerNamed( "md5", StandardSpiBasicTypes.STRING );

		registry.registerNamed( "concat", StandardSpiBasicTypes.STRING );

		registry.registerNamed( "substring", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "substr", StandardSpiBasicTypes.STRING );

		registry.registerNamed( "length", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "bit_length", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "coalesce" );
		registry.registerNamed( "nullif" );
		registry.registerNamed( "mod" );

		registry.registerNamed( "power" );
		registry.registerNamed( "stddev" );
		registry.registerNamed( "variance" );
		registry.registerNamed( "trunc" );
		registry.registerNamed( "nvl" );
		registry.registerNamed( "nvl2" );
		registry.registerNamed( "chr", StandardSpiBasicTypes.CHARACTER );
		registry.registerNamed( "to_char", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "to_date", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerNamed( "instr", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "instrb", StandardSpiBasicTypes.INTEGER );
		registry.registerNamed( "lpad", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "replace", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "rpad", StandardSpiBasicTypes.STRING );
		registry.registerNamed( "translate", StandardSpiBasicTypes.STRING );

		registry.registerNamed( "add_months", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "user", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "rownum", StandardSpiBasicTypes.LONG );
		registry.registerVarArgs( "concat", StandardSpiBasicTypes.STRING, "", "||", "" );
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
