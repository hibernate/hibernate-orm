/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
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

		registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "bin", new StandardSQLFunction( "bin", StandardSpiBasicTypes.STRING ) );
		registerFunction( "char_length", new StandardSQLFunction( "char_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "character_length", new StandardSQLFunction( "character_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "lengthb", new StandardSQLFunction( "lengthb", StandardSpiBasicTypes.LONG ) );
		registerFunction( "lengthh", new StandardSQLFunction( "lengthh", StandardSpiBasicTypes.LONG ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase" ) );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
		registerFunction( "reverse", new StandardSQLFunction( "reverse" ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim" ) );
		registerFunction( "trim", new StandardSQLFunction( "trim" ) );
		registerFunction( "space", new StandardSQLFunction( "space", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ucase", new StandardSQLFunction( "ucase" ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "acos", new StandardSQLFunction( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSQLFunction( "cot", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "ln", new StandardSQLFunction( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log2", new StandardSQLFunction( "log2", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardSQLFunction( "log10", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgSQLFunction( "pi", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "rand", new NoArgSQLFunction( "rand", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "random", new NoArgSQLFunction( "random", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "radians", new StandardSQLFunction( "radians", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "ceil", new StandardSQLFunction( "ceil", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "floor", new StandardSQLFunction( "floor", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "round", new StandardSQLFunction( "round" ) );

		registerFunction( "datediff", new StandardSQLFunction( "datediff", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "timediff", new StandardSQLFunction( "timediff", StandardSpiBasicTypes.TIME ) );

		registerFunction( "date", new StandardSQLFunction( "date", StandardSpiBasicTypes.DATE ) );
		registerFunction( "curdate", new NoArgSQLFunction( "curdate", StandardSpiBasicTypes.DATE ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "sys_date", new NoArgSQLFunction( "sys_date", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "sysdate", new NoArgSQLFunction( "sysdate", StandardSpiBasicTypes.DATE, false ) );

		registerFunction( "time", new StandardSQLFunction( "time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "curtime", new NoArgSQLFunction( "curtime", StandardSpiBasicTypes.TIME ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardSpiBasicTypes.TIME, false ) );
		registerFunction( "sys_time", new NoArgSQLFunction( "sys_time", StandardSpiBasicTypes.TIME, false ) );
		registerFunction( "systime", new NoArgSQLFunction( "systime", StandardSpiBasicTypes.TIME, false ) );

		registerFunction( "timestamp", new StandardSQLFunction( "timestamp", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction(
				"current_timestamp", new NoArgSQLFunction(
						"current_timestamp",
						StandardSpiBasicTypes.TIMESTAMP,
						false
		)
		);
		registerFunction(
				"sys_timestamp", new NoArgSQLFunction(
						"sys_timestamp",
						StandardSpiBasicTypes.TIMESTAMP,
						false
		)
		);
		registerFunction( "systimestamp", new NoArgSQLFunction( "systimestamp", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction( "localtime", new NoArgSQLFunction( "localtime", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction(
				"localtimestamp", new NoArgSQLFunction(
						"localtimestamp",
						StandardSpiBasicTypes.TIMESTAMP,
						false
		)
		);

		registerFunction( "day", new StandardSQLFunction( "day", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "from_days", new StandardSQLFunction( "from_days", StandardSpiBasicTypes.DATE ) );
		registerFunction( "from_unixtime", new StandardSQLFunction( "from_unixtime", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "last_day", new StandardSQLFunction( "last_day", StandardSpiBasicTypes.DATE ) );
		registerFunction( "minute", new StandardSQLFunction( "minute", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "months_between", new StandardSQLFunction( "months_between", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "now", new NoArgSQLFunction( "now", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "quarter", new StandardSQLFunction( "quarter", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "second", new StandardSQLFunction( "second", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "sec_to_time", new StandardSQLFunction( "sec_to_time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "time_to_sec", new StandardSQLFunction( "time_to_sec", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "to_days", new StandardSQLFunction( "to_days", StandardSpiBasicTypes.LONG ) );
		registerFunction( "unix_timestamp", new StandardSQLFunction( "unix_timestamp", StandardSpiBasicTypes.LONG ) );
		registerFunction( "utc_date", new NoArgSQLFunction( "utc_date", StandardSpiBasicTypes.STRING ) );
		registerFunction( "utc_time", new NoArgSQLFunction( "utc_time", StandardSpiBasicTypes.STRING ) );
		registerFunction( "week", new StandardSQLFunction( "week", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "weekday", new StandardSQLFunction( "weekday", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "year", new StandardSQLFunction( "year", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "hex", new StandardSQLFunction( "hex", StandardSpiBasicTypes.STRING ) );

		registerFunction( "octet_length", new StandardSQLFunction( "octet_length", StandardSpiBasicTypes.LONG ) );
		registerFunction( "bit_length", new StandardSQLFunction( "bit_length", StandardSpiBasicTypes.LONG ) );

		registerFunction( "bit_count", new StandardSQLFunction( "bit_count", StandardSpiBasicTypes.LONG ) );
		registerFunction( "md5", new StandardSQLFunction( "md5", StandardSpiBasicTypes.STRING ) );

		registerFunction( "concat", new StandardSQLFunction( "concat", StandardSpiBasicTypes.STRING ) );

		registerFunction( "substring", new StandardSQLFunction( "substring", StandardSpiBasicTypes.STRING ) );
		registerFunction( "substr", new StandardSQLFunction( "substr", StandardSpiBasicTypes.STRING ) );

		registerFunction( "length", new StandardSQLFunction( "length", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "bit_length", new StandardSQLFunction( "bit_length", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "coalesce", new StandardSQLFunction( "coalesce" ) );
		registerFunction( "nullif", new StandardSQLFunction( "nullif" ) );
		registerFunction( "mod", new StandardSQLFunction( "mod" ) );

		registerFunction( "power", new StandardSQLFunction( "power" ) );
		registerFunction( "stddev", new StandardSQLFunction( "stddev" ) );
		registerFunction( "variance", new StandardSQLFunction( "variance" ) );
		registerFunction( "trunc", new StandardSQLFunction( "trunc" ) );
		registerFunction( "nvl", new StandardSQLFunction( "nvl" ) );
		registerFunction( "nvl2", new StandardSQLFunction( "nvl2" ) );
		registerFunction( "chr", new StandardSQLFunction( "chr", StandardSpiBasicTypes.CHARACTER ) );
		registerFunction( "to_char", new StandardSQLFunction( "to_char", StandardSpiBasicTypes.STRING ) );
		registerFunction( "to_date", new StandardSQLFunction( "to_date", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "instr", new StandardSQLFunction( "instr", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "instrb", new StandardSQLFunction( "instrb", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "lpad", new StandardSQLFunction( "lpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSQLFunction( "replace", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSQLFunction( "rpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "translate", new StandardSQLFunction( "translate", StandardSpiBasicTypes.STRING ) );

		registerFunction( "add_months", new StandardSQLFunction( "add_months", StandardSpiBasicTypes.DATE ) );
		registerFunction( "user", new NoArgSQLFunction( "user", StandardSpiBasicTypes.STRING, false ) );
		registerFunction( "rownum", new NoArgSQLFunction( "rownum", StandardSpiBasicTypes.LONG, false ) );
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
