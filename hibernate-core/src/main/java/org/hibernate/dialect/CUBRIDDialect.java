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
import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
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

		registerFunction( "ascii", new NamedSqmFunctionTemplate( "ascii", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "bin", new NamedSqmFunctionTemplate( "bin", StandardSpiBasicTypes.STRING ) );
		registerFunction( "char_length", new NamedSqmFunctionTemplate( "char_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "character_length", new NamedSqmFunctionTemplate( "character_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "lengthb", new NamedSqmFunctionTemplate( "lengthb", StandardSpiBasicTypes.LONG ) );
		registerFunction( "lengthh", new NamedSqmFunctionTemplate( "lengthh", StandardSpiBasicTypes.LONG ) );
		registerFunction( "lcase", new NamedSqmFunctionTemplate( "lcase" ) );
		registerFunction( "lower", new NamedSqmFunctionTemplate( "lower" ) );
		registerFunction( "ltrim", new NamedSqmFunctionTemplate( "ltrim" ) );
		registerFunction( "reverse", new NamedSqmFunctionTemplate( "reverse" ) );
		registerFunction( "rtrim", new NamedSqmFunctionTemplate( "rtrim" ) );
		registerFunction( "trim", new NamedSqmFunctionTemplate( "trim" ) );
		registerFunction( "space", new NamedSqmFunctionTemplate( "space", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ucase", new NamedSqmFunctionTemplate( "ucase" ) );
		registerFunction( "upper", new NamedSqmFunctionTemplate( "upper" ) );

		registerFunction( "abs", new NamedSqmFunctionTemplate( "abs" ) );
		registerFunction( "sign", new NamedSqmFunctionTemplate( "sign", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "acos", new NamedSqmFunctionTemplate( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new NamedSqmFunctionTemplate( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new NamedSqmFunctionTemplate( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new NamedSqmFunctionTemplate( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cot", new NamedSqmFunctionTemplate( "cot", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "exp", new NamedSqmFunctionTemplate( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "ln", new NamedSqmFunctionTemplate( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log2", new NamedSqmFunctionTemplate( "log2", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log10", new NamedSqmFunctionTemplate( "log10", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgsSqmFunctionTemplate( "pi", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "rand", new NoArgsSqmFunctionTemplate( "rand", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "random", new NoArgsSqmFunctionTemplate( "random", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sin", new NamedSqmFunctionTemplate( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new NamedSqmFunctionTemplate( "sqrt", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tan", new NamedSqmFunctionTemplate( "tan", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "radians", new NamedSqmFunctionTemplate( "radians", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new NamedSqmFunctionTemplate( "degrees", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "ceil", new NamedSqmFunctionTemplate( "ceil", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "floor", new NamedSqmFunctionTemplate( "floor", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "round", new NamedSqmFunctionTemplate( "round" ) );

		registerFunction( "datediff", new NamedSqmFunctionTemplate( "datediff", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "timediff", new NamedSqmFunctionTemplate( "timediff", StandardSpiBasicTypes.TIME ) );

		registerFunction( "date", new NamedSqmFunctionTemplate( "date", StandardSpiBasicTypes.DATE ) );
		registerFunction( "curdate", new NoArgsSqmFunctionTemplate( "curdate", StandardSpiBasicTypes.DATE ) );
		registerFunction( "current_date", new NoArgsSqmFunctionTemplate( "current_date", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "sys_date", new NoArgsSqmFunctionTemplate( "sys_date", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "sysdate", new NoArgsSqmFunctionTemplate( "sysdate", StandardSpiBasicTypes.DATE, false ) );

		registerFunction( "time", new NamedSqmFunctionTemplate( "time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "curtime", new NoArgsSqmFunctionTemplate( "curtime", StandardSpiBasicTypes.TIME ) );
		registerFunction( "current_time", new NoArgsSqmFunctionTemplate( "current_time", StandardSpiBasicTypes.TIME, false ) );
		registerFunction( "sys_time", new NoArgsSqmFunctionTemplate( "sys_time", StandardSpiBasicTypes.TIME, false ) );
		registerFunction( "systime", new NoArgsSqmFunctionTemplate( "systime", StandardSpiBasicTypes.TIME, false ) );

		registerFunction( "timestamp", new NamedSqmFunctionTemplate( "timestamp", StandardSpiBasicTypes.TIMESTAMP ) );
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

		registerFunction( "day", new NamedSqmFunctionTemplate( "day", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofmonth", new NamedSqmFunctionTemplate( "dayofmonth", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new NamedSqmFunctionTemplate( "dayofweek", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new NamedSqmFunctionTemplate( "dayofyear", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "from_days", new NamedSqmFunctionTemplate( "from_days", StandardSpiBasicTypes.DATE ) );
		registerFunction( "from_unixtime", new NamedSqmFunctionTemplate( "from_unixtime", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "last_day", new NamedSqmFunctionTemplate( "last_day", StandardSpiBasicTypes.DATE ) );
		registerFunction( "minute", new NamedSqmFunctionTemplate( "minute", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "month", new NamedSqmFunctionTemplate( "month", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "months_between", new NamedSqmFunctionTemplate( "months_between", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "now", new NoArgsSqmFunctionTemplate( "now", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "quarter", new NamedSqmFunctionTemplate( "quarter", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "second", new NamedSqmFunctionTemplate( "second", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "sec_to_time", new NamedSqmFunctionTemplate( "sec_to_time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "time_to_sec", new NamedSqmFunctionTemplate( "time_to_sec", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "to_days", new NamedSqmFunctionTemplate( "to_days", StandardSpiBasicTypes.LONG ) );
		registerFunction( "unix_timestamp", new NamedSqmFunctionTemplate( "unix_timestamp", StandardSpiBasicTypes.LONG ) );
		registerFunction( "utc_date", new NoArgsSqmFunctionTemplate( "utc_date", StandardSpiBasicTypes.STRING ) );
		registerFunction( "utc_time", new NoArgsSqmFunctionTemplate( "utc_time", StandardSpiBasicTypes.STRING ) );
		registerFunction( "week", new NamedSqmFunctionTemplate( "week", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "weekday", new NamedSqmFunctionTemplate( "weekday", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "year", new NamedSqmFunctionTemplate( "year", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "hex", new NamedSqmFunctionTemplate( "hex", StandardSpiBasicTypes.STRING ) );

		registerFunction( "octet_length", new NamedSqmFunctionTemplate( "octet_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "bit_length", new NamedSqmFunctionTemplate( "bit_length", StandardSpiBasicTypes.LONG ) );

		registerFunction( "bit_count", new NamedSqmFunctionTemplate( "bit_count", StandardSpiBasicTypes.LONG ) );
		registerFunction( "md5", new NamedSqmFunctionTemplate( "md5", StandardSpiBasicTypes.STRING ) );

		registerFunction( "concat", new NamedSqmFunctionTemplate( "concat", StandardSpiBasicTypes.STRING ) );

		registerFunction( "substring", new NamedSqmFunctionTemplate( "substring", StandardSpiBasicTypes.STRING ) );
		registerFunction( "substr", new NamedSqmFunctionTemplate( "substr", StandardSpiBasicTypes.STRING ) );

		registerFunction( "length", new NamedSqmFunctionTemplate( "length", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "bit_length", new NamedSqmFunctionTemplate( "bit_length", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "coalesce", new NamedSqmFunctionTemplate( "coalesce" ) );
		registerFunction( "nullif", new NamedSqmFunctionTemplate( "nullif" ) );
		registerFunction( "mod", new NamedSqmFunctionTemplate( "mod" ) );

		registerFunction( "power", new NamedSqmFunctionTemplate( "power" ) );
		registerFunction( "stddev", new NamedSqmFunctionTemplate( "stddev" ) );
		registerFunction( "variance", new NamedSqmFunctionTemplate( "variance" ) );
		registerFunction( "trunc", new NamedSqmFunctionTemplate( "trunc" ) );
		registerFunction( "nvl", new NamedSqmFunctionTemplate( "nvl" ) );
		registerFunction( "nvl2", new NamedSqmFunctionTemplate( "nvl2" ) );
		registerFunction( "chr", new NamedSqmFunctionTemplate( "chr", StandardSpiBasicTypes.CHARACTER ) );
		registerFunction( "to_char", new NamedSqmFunctionTemplate( "to_char", StandardSpiBasicTypes.STRING ) );
		registerFunction( "to_date", new NamedSqmFunctionTemplate( "to_date", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "instr", new NamedSqmFunctionTemplate( "instr", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "instrb", new NamedSqmFunctionTemplate( "instrb", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "lpad", new NamedSqmFunctionTemplate( "lpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "replace", new NamedSqmFunctionTemplate( "replace", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rpad", new NamedSqmFunctionTemplate( "rpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "translate", new NamedSqmFunctionTemplate( "translate", StandardSpiBasicTypes.STRING ) );

		registerFunction( "add_months", new NamedSqmFunctionTemplate( "add_months", StandardSpiBasicTypes.DATE ) );
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
