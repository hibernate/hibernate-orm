/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgsSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.StandardSqmFunctionTemplate;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.identity.CUBRIDIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.CUBRIDLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
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

		registerFunction( "ascii", new StandardSqmFunctionTemplate( "ascii", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "bin", new StandardSqmFunctionTemplate( "bin", StandardSpiBasicTypes.STRING ) );
		registerFunction( "char_length", new StandardSqmFunctionTemplate( "char_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "character_length", new StandardSqmFunctionTemplate( "character_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "lengthb", new StandardSqmFunctionTemplate( "lengthb", StandardSpiBasicTypes.LONG ) );
		registerFunction( "lengthh", new StandardSqmFunctionTemplate( "lengthh", StandardSpiBasicTypes.LONG ) );
		registerFunction( "lcase", new StandardSqmFunctionTemplate( "lcase" ) );
		registerFunction( "lower", new StandardSqmFunctionTemplate( "lower" ) );
		registerFunction( "ltrim", new StandardSqmFunctionTemplate( "ltrim" ) );
		registerFunction( "reverse", new StandardSqmFunctionTemplate( "reverse" ) );
		registerFunction( "rtrim", new StandardSqmFunctionTemplate( "rtrim" ) );
		registerFunction( "trim", new StandardSqmFunctionTemplate( "trim" ) );
		registerFunction( "space", new StandardSqmFunctionTemplate( "space", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ucase", new StandardSqmFunctionTemplate( "ucase" ) );
		registerFunction( "upper", new StandardSqmFunctionTemplate( "upper" ) );

		registerFunction( "abs", new StandardSqmFunctionTemplate( "abs" ) );
		registerFunction( "sign", new StandardSqmFunctionTemplate( "sign", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "acos", new StandardSqmFunctionTemplate( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSqmFunctionTemplate( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSqmFunctionTemplate( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSqmFunctionTemplate( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSqmFunctionTemplate( "cot", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "exp", new StandardSqmFunctionTemplate( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "ln", new StandardSqmFunctionTemplate( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log2", new StandardSqmFunctionTemplate( "log2", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardSqmFunctionTemplate( "log10", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgsSqmFunctionTemplate( "pi", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "rand", new NoArgsSqmFunctionTemplate( "rand", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "random", new NoArgsSqmFunctionTemplate( "random", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSqmFunctionTemplate( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new StandardSqmFunctionTemplate( "sqrt", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSqmFunctionTemplate( "tan", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "radians", new StandardSqmFunctionTemplate( "radians", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new StandardSqmFunctionTemplate( "degrees", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "ceil", new StandardSqmFunctionTemplate( "ceil", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "floor", new StandardSqmFunctionTemplate( "floor", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "round", new StandardSqmFunctionTemplate( "round" ) );

		registerFunction( "datediff", new StandardSqmFunctionTemplate( "datediff", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "timediff", new StandardSqmFunctionTemplate( "timediff", StandardSpiBasicTypes.TIME ) );

		registerFunction( "date", new StandardSqmFunctionTemplate( "date", StandardSpiBasicTypes.DATE ) );
		registerFunction( "curdate", new NoArgsSqmFunctionTemplate( "curdate", StandardSpiBasicTypes.DATE ) );
		registerFunction( "current_date", new NoArgsSqmFunctionTemplate( "current_date", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "sys_date", new NoArgsSqmFunctionTemplate( "sys_date", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "sysdate", new NoArgsSqmFunctionTemplate( "sysdate", StandardSpiBasicTypes.DATE, false ) );

		registerFunction( "time", new StandardSqmFunctionTemplate( "time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "curtime", new NoArgsSqmFunctionTemplate( "curtime", StandardSpiBasicTypes.TIME ) );
		registerFunction( "current_time", new NoArgsSqmFunctionTemplate( "current_time", StandardSpiBasicTypes.TIME, false ) );
		registerFunction( "sys_time", new NoArgsSqmFunctionTemplate( "sys_time", StandardSpiBasicTypes.TIME, false ) );
		registerFunction( "systime", new NoArgsSqmFunctionTemplate( "systime", StandardSpiBasicTypes.TIME, false ) );

		registerFunction( "timestamp", new StandardSqmFunctionTemplate( "timestamp", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction(
				"current_timestamp", new NoArgsSqmFunctionTemplate(
						"current_timestamp",
						StandardSpiBasicTypes.TIMESTAMP,
						false
		)
		);
		registerFunction(
				"sys_timestamp", new NoArgsSqmFunctionTemplate(
						"sys_timestamp",
						StandardSpiBasicTypes.TIMESTAMP,
						false
		)
		);
		registerFunction( "systimestamp", new NoArgsSqmFunctionTemplate( "systimestamp", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction( "localtime", new NoArgsSqmFunctionTemplate( "localtime", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction(
				"localtimestamp", new NoArgsSqmFunctionTemplate(
						"localtimestamp",
						StandardSpiBasicTypes.TIMESTAMP,
						false
		)
		);

		registerFunction( "day", new StandardSqmFunctionTemplate( "day", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofmonth", new StandardSqmFunctionTemplate( "dayofmonth", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new StandardSqmFunctionTemplate( "dayofweek", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSqmFunctionTemplate( "dayofyear", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "from_days", new StandardSqmFunctionTemplate( "from_days", StandardSpiBasicTypes.DATE ) );
		registerFunction( "from_unixtime", new StandardSqmFunctionTemplate( "from_unixtime", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "last_day", new StandardSqmFunctionTemplate( "last_day", StandardSpiBasicTypes.DATE ) );
		registerFunction( "minute", new StandardSqmFunctionTemplate( "minute", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSqmFunctionTemplate( "month", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "months_between", new StandardSqmFunctionTemplate( "months_between", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "now", new NoArgsSqmFunctionTemplate( "now", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "quarter", new StandardSqmFunctionTemplate( "quarter", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "second", new StandardSqmFunctionTemplate( "second", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "sec_to_time", new StandardSqmFunctionTemplate( "sec_to_time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "time_to_sec", new StandardSqmFunctionTemplate( "time_to_sec", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "to_days", new StandardSqmFunctionTemplate( "to_days", StandardSpiBasicTypes.LONG ) );
		registerFunction( "unix_timestamp", new StandardSqmFunctionTemplate( "unix_timestamp", StandardSpiBasicTypes.LONG ) );
		registerFunction( "utc_date", new NoArgsSqmFunctionTemplate( "utc_date", StandardSpiBasicTypes.STRING ) );
		registerFunction( "utc_time", new NoArgsSqmFunctionTemplate( "utc_time", StandardSpiBasicTypes.STRING ) );
		registerFunction( "week", new StandardSqmFunctionTemplate( "week", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "weekday", new StandardSqmFunctionTemplate( "weekday", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "year", new StandardSqmFunctionTemplate( "year", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "hex", new StandardSqmFunctionTemplate( "hex", StandardSpiBasicTypes.STRING ) );

		registerFunction( "octet_length", new StandardSqmFunctionTemplate( "octet_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "bit_length", new StandardSqmFunctionTemplate( "bit_length", StandardSpiBasicTypes.LONG ) );

		registerFunction( "bit_count", new StandardSqmFunctionTemplate( "bit_count", StandardSpiBasicTypes.LONG ) );
		registerFunction( "md5", new StandardSqmFunctionTemplate( "md5", StandardSpiBasicTypes.STRING ) );

		registerFunction( "concat", new StandardSqmFunctionTemplate( "concat", StandardSpiBasicTypes.STRING ) );

		registerFunction( "substring", new StandardSqmFunctionTemplate( "substring", StandardSpiBasicTypes.STRING ) );
		registerFunction( "substr", new StandardSqmFunctionTemplate( "substr", StandardSpiBasicTypes.STRING ) );

		registerFunction( "length", new StandardSqmFunctionTemplate( "length", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "bit_length", new StandardSqmFunctionTemplate( "bit_length", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "coalesce", new StandardSqmFunctionTemplate( "coalesce" ) );
		registerFunction( "nullif", new StandardSqmFunctionTemplate( "nullif" ) );
		registerFunction( "mod", new StandardSqmFunctionTemplate( "mod" ) );

		registerFunction( "power", new StandardSqmFunctionTemplate( "power" ) );
		registerFunction( "stddev", new StandardSqmFunctionTemplate( "stddev" ) );
		registerFunction( "variance", new StandardSqmFunctionTemplate( "variance" ) );
		registerFunction( "trunc", new StandardSqmFunctionTemplate( "trunc" ) );
		registerFunction( "nvl", new StandardSqmFunctionTemplate( "nvl" ) );
		registerFunction( "nvl2", new StandardSqmFunctionTemplate( "nvl2" ) );
		registerFunction( "chr", new StandardSqmFunctionTemplate( "chr", StandardSpiBasicTypes.CHARACTER ) );
		registerFunction( "to_char", new StandardSqmFunctionTemplate( "to_char", StandardSpiBasicTypes.STRING ) );
		registerFunction( "to_date", new StandardSqmFunctionTemplate( "to_date", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "instr", new StandardSqmFunctionTemplate( "instr", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "instrb", new StandardSqmFunctionTemplate( "instrb", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "lpad", new StandardSqmFunctionTemplate( "lpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSqmFunctionTemplate( "replace", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSqmFunctionTemplate( "rpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "translate", new StandardSqmFunctionTemplate( "translate", StandardSpiBasicTypes.STRING ) );

		registerFunction( "add_months", new StandardSqmFunctionTemplate( "add_months", StandardSpiBasicTypes.DATE ) );
		registerFunction( "user", new NoArgsSqmFunctionTemplate( "user", StandardSpiBasicTypes.STRING, false ) );
		registerFunction( "rownum", new NoArgsSqmFunctionTemplate( "rownum", StandardSpiBasicTypes.LONG, false ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardSpiBasicTypes.STRING, "", "||", "" ) );

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
		return "select name from db_serial";
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
