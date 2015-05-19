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
import org.hibernate.dialect.pagination.CUBRIDLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.type.StandardBasicTypes;

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

		registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardBasicTypes.INTEGER ) );
		registerFunction( "bin", new StandardSQLFunction( "bin", StandardBasicTypes.STRING ) );
		registerFunction( "char_length", new StandardSQLFunction( "char_length", StandardBasicTypes.LONG ) );
		registerFunction( "character_length", new StandardSQLFunction( "character_length", StandardBasicTypes.LONG ) );
		registerFunction( "lengthb", new StandardSQLFunction( "lengthb", StandardBasicTypes.LONG ) );
		registerFunction( "lengthh", new StandardSQLFunction( "lengthh", StandardBasicTypes.LONG ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase" ) );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
		registerFunction( "reverse", new StandardSQLFunction( "reverse" ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim" ) );
		registerFunction( "trim", new StandardSQLFunction( "trim" ) );
		registerFunction( "space", new StandardSQLFunction( "space", StandardBasicTypes.STRING ) );
		registerFunction( "ucase", new StandardSQLFunction( "ucase" ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardBasicTypes.INTEGER ) );

		registerFunction( "acos", new StandardSQLFunction( "acos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSQLFunction( "cot", StandardBasicTypes.DOUBLE ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "ln", new StandardSQLFunction( "ln", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log2", new StandardSQLFunction( "log2", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardSQLFunction( "log10", StandardBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgSQLFunction( "pi", StandardBasicTypes.DOUBLE ) );
		registerFunction( "rand", new NoArgSQLFunction( "rand", StandardBasicTypes.DOUBLE ) );
		registerFunction( "random", new NoArgSQLFunction( "random", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardBasicTypes.DOUBLE ) );

		registerFunction( "radians", new StandardSQLFunction( "radians", StandardBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", StandardBasicTypes.DOUBLE ) );

		registerFunction( "ceil", new StandardSQLFunction( "ceil", StandardBasicTypes.INTEGER ) );
		registerFunction( "floor", new StandardSQLFunction( "floor", StandardBasicTypes.INTEGER ) );
		registerFunction( "round", new StandardSQLFunction( "round" ) );

		registerFunction( "datediff", new StandardSQLFunction( "datediff", StandardBasicTypes.INTEGER ) );
		registerFunction( "timediff", new StandardSQLFunction( "timediff", StandardBasicTypes.TIME ) );

		registerFunction( "date", new StandardSQLFunction( "date", StandardBasicTypes.DATE ) );
		registerFunction( "curdate", new NoArgSQLFunction( "curdate", StandardBasicTypes.DATE ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE, false ) );
		registerFunction( "sys_date", new NoArgSQLFunction( "sys_date", StandardBasicTypes.DATE, false ) );
		registerFunction( "sysdate", new NoArgSQLFunction( "sysdate", StandardBasicTypes.DATE, false ) );

		registerFunction( "time", new StandardSQLFunction( "time", StandardBasicTypes.TIME ) );
		registerFunction( "curtime", new NoArgSQLFunction( "curtime", StandardBasicTypes.TIME ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardBasicTypes.TIME, false ) );
		registerFunction( "sys_time", new NoArgSQLFunction( "sys_time", StandardBasicTypes.TIME, false ) );
		registerFunction( "systime", new NoArgSQLFunction( "systime", StandardBasicTypes.TIME, false ) );

		registerFunction( "timestamp", new StandardSQLFunction( "timestamp", StandardBasicTypes.TIMESTAMP ) );
		registerFunction(
				"current_timestamp", new NoArgSQLFunction(
				"current_timestamp",
				StandardBasicTypes.TIMESTAMP,
				false
		)
		);
		registerFunction(
				"sys_timestamp", new NoArgSQLFunction(
				"sys_timestamp",
				StandardBasicTypes.TIMESTAMP,
				false
		)
		);
		registerFunction( "systimestamp", new NoArgSQLFunction( "systimestamp", StandardBasicTypes.TIMESTAMP, false ) );
		registerFunction( "localtime", new NoArgSQLFunction( "localtime", StandardBasicTypes.TIMESTAMP, false ) );
		registerFunction(
				"localtimestamp", new NoArgSQLFunction(
				"localtimestamp",
				StandardBasicTypes.TIMESTAMP,
				false
		)
		);

		registerFunction( "day", new StandardSQLFunction( "day", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardBasicTypes.INTEGER ) );
		registerFunction( "from_days", new StandardSQLFunction( "from_days", StandardBasicTypes.DATE ) );
		registerFunction( "from_unixtime", new StandardSQLFunction( "from_unixtime", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "last_day", new StandardSQLFunction( "last_day", StandardBasicTypes.DATE ) );
		registerFunction( "minute", new StandardSQLFunction( "minute", StandardBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", StandardBasicTypes.INTEGER ) );
		registerFunction( "months_between", new StandardSQLFunction( "months_between", StandardBasicTypes.DOUBLE ) );
		registerFunction( "now", new NoArgSQLFunction( "now", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "quarter", new StandardSQLFunction( "quarter", StandardBasicTypes.INTEGER ) );
		registerFunction( "second", new StandardSQLFunction( "second", StandardBasicTypes.INTEGER ) );
		registerFunction( "sec_to_time", new StandardSQLFunction( "sec_to_time", StandardBasicTypes.TIME ) );
		registerFunction( "time_to_sec", new StandardSQLFunction( "time_to_sec", StandardBasicTypes.INTEGER ) );
		registerFunction( "to_days", new StandardSQLFunction( "to_days", StandardBasicTypes.LONG ) );
		registerFunction( "unix_timestamp", new StandardSQLFunction( "unix_timestamp", StandardBasicTypes.LONG ) );
		registerFunction( "utc_date", new NoArgSQLFunction( "utc_date", StandardBasicTypes.STRING ) );
		registerFunction( "utc_time", new NoArgSQLFunction( "utc_time", StandardBasicTypes.STRING ) );
		registerFunction( "week", new StandardSQLFunction( "week", StandardBasicTypes.INTEGER ) );
		registerFunction( "weekday", new StandardSQLFunction( "weekday", StandardBasicTypes.INTEGER ) );
		registerFunction( "year", new StandardSQLFunction( "year", StandardBasicTypes.INTEGER ) );

		registerFunction( "hex", new StandardSQLFunction( "hex", StandardBasicTypes.STRING ) );

		registerFunction( "octet_length", new StandardSQLFunction( "octet_length", StandardBasicTypes.LONG ) );
		registerFunction( "bit_length", new StandardSQLFunction( "bit_length", StandardBasicTypes.LONG ) );

		registerFunction( "bit_count", new StandardSQLFunction( "bit_count", StandardBasicTypes.LONG ) );
		registerFunction( "md5", new StandardSQLFunction( "md5", StandardBasicTypes.STRING ) );

		registerFunction( "concat", new StandardSQLFunction( "concat", StandardBasicTypes.STRING ) );

		registerFunction( "substring", new StandardSQLFunction( "substring", StandardBasicTypes.STRING ) );
		registerFunction( "substr", new StandardSQLFunction( "substr", StandardBasicTypes.STRING ) );

		registerFunction( "length", new StandardSQLFunction( "length", StandardBasicTypes.INTEGER ) );
		registerFunction( "bit_length", new StandardSQLFunction( "bit_length", StandardBasicTypes.INTEGER ) );
		registerFunction( "coalesce", new StandardSQLFunction( "coalesce" ) );
		registerFunction( "nullif", new StandardSQLFunction( "nullif" ) );
		registerFunction( "mod", new StandardSQLFunction( "mod" ) );

		registerFunction( "power", new StandardSQLFunction( "power" ) );
		registerFunction( "stddev", new StandardSQLFunction( "stddev" ) );
		registerFunction( "variance", new StandardSQLFunction( "variance" ) );
		registerFunction( "trunc", new StandardSQLFunction( "trunc" ) );
		registerFunction( "nvl", new StandardSQLFunction( "nvl" ) );
		registerFunction( "nvl2", new StandardSQLFunction( "nvl2" ) );
		registerFunction( "chr", new StandardSQLFunction( "chr", StandardBasicTypes.CHARACTER ) );
		registerFunction( "to_char", new StandardSQLFunction( "to_char", StandardBasicTypes.STRING ) );
		registerFunction( "to_date", new StandardSQLFunction( "to_date", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "instr", new StandardSQLFunction( "instr", StandardBasicTypes.INTEGER ) );
		registerFunction( "instrb", new StandardSQLFunction( "instrb", StandardBasicTypes.INTEGER ) );
		registerFunction( "lpad", new StandardSQLFunction( "lpad", StandardBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSQLFunction( "rpad", StandardBasicTypes.STRING ) );
		registerFunction( "translate", new StandardSQLFunction( "translate", StandardBasicTypes.STRING ) );

		registerFunction( "add_months", new StandardSQLFunction( "add_months", StandardBasicTypes.DATE ) );
		registerFunction( "user", new NoArgSQLFunction( "user", StandardBasicTypes.STRING, false ) );
		registerFunction( "rownum", new NoArgSQLFunction( "rownum", StandardBasicTypes.LONG, false ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "", "||", "" ) );

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
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentityInsertString() {
		return "NULL";
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
	public String getIdentitySelectString() {
		return "select last_insert_id()";
	}

	@Override
	protected String getIdentityColumnString() {
		//starts with 1, implicitly
		return "not null auto_increment";
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
}
