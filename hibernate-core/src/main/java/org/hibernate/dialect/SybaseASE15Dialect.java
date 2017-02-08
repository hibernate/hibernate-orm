/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.function.AnsiTrimEmulationFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.TinyIntSqlDescriptor;

/**
 * An SQL dialect targeting Sybase Adaptive Server Enterprise (ASE) 15 and higher.
 * <p/>
 * TODO : verify if this also works with 12/12.5
 *
 * @author Gavin King
 */
public class SybaseASE15Dialect extends SybaseDialect {
	/**
	 * Constructs a SybaseASE15Dialect
	 */
	public SybaseASE15Dialect() {
		super();

		registerColumnType( Types.LONGVARBINARY, "image" );
		registerColumnType( Types.LONGVARCHAR, "text" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.DECIMAL, "numeric($p,$s)" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.REAL, "real" );
		registerColumnType( Types.BOOLEAN, "tinyint" );

		registerFunction( "second", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "datepart(second, ?1)" ) );
		registerFunction( "minute", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "datepart(minute, ?1)" ) );
		registerFunction( "hour", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "datepart(hour, ?1)" ) );
		registerFunction( "extract", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "datepart(?1, ?3)" ) );
		registerFunction( "mod", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "?1 % ?2" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "datalength(?1) * 8" ) );
		registerFunction(
				"trim", new AnsiTrimEmulationFunction(
						AnsiTrimEmulationFunction.LTRIM, AnsiTrimEmulationFunction.RTRIM, "str_replace"
				)
		);

		registerFunction( "atan2", new SQLFunctionTemplate( StandardSpiBasicTypes.DOUBLE, "atn2(?1, ?2)" ) );
		registerFunction( "atn2", new SQLFunctionTemplate( StandardSpiBasicTypes.DOUBLE, "atn2(?1, ?2)" ) );

		registerFunction( "biginttohex", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "biginttohext(?1)" ) );
		registerFunction( "char_length", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "char_length(?1)" ) );
		registerFunction( "charindex", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "charindex(?1, ?2)" ) );
		registerFunction( "coalesce", new VarArgsSQLFunction( "coalesce(", ",", ")" ) );
		registerFunction( "col_length", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "col_length(?1, ?2)" ) );
		registerFunction( "col_name", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "col_name(?1, ?2)" ) );
		// Sybase has created current_date and current_time inplace of getdate()
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardSpiBasicTypes.DATE ) );


		registerFunction( "data_pages", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "data_pages(?1, ?2)" ) );
		registerFunction(
				"data_pages", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "data_pages(?1, ?2, ?3)" )
		);
		registerFunction(
				"data_pages", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "data_pages(?1, ?2, ?3, ?4)" )
		);
		registerFunction( "datalength", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "datalength(?1)" ) );
		registerFunction( "dateadd", new SQLFunctionTemplate( StandardSpiBasicTypes.TIMESTAMP, "dateadd(?1, ?2, ?3)" ) );
		registerFunction( "datediff", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "datediff(?1, ?2, ?3)" ) );
		registerFunction( "datepart", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "datepart(?1, ?2)" ) );
		registerFunction( "datetime", new SQLFunctionTemplate( StandardSpiBasicTypes.TIMESTAMP, "datetime" ) );
		registerFunction( "db_id", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "db_id(?1)" ) );
		registerFunction( "difference", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "difference(?1,?2)" ) );
		registerFunction( "db_name", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "db_name(?1)" ) );
		registerFunction( "has_role", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "has_role(?1, ?2)" ) );
		registerFunction( "hextobigint", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "hextobigint(?1)" ) );
		registerFunction( "hextoint", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "hextoint(?1)" ) );
		registerFunction( "host_id", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "host_id" ) );
		registerFunction( "host_name", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "host_name" ) );
		registerFunction( "inttohex", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "inttohex(?1)" ) );
		registerFunction( "is_quiesced", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "is_quiesced(?1)" ) );
		registerFunction(
				"is_sec_service_on", new SQLFunctionTemplate( StandardSpiBasicTypes.BOOLEAN, "is_sec_service_on(?1)" )
		);
		registerFunction( "object_id", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "object_id(?1)" ) );
		registerFunction( "object_name", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "object_name(?1)" ) );
		registerFunction( "pagesize", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "pagesize(?1)" ) );
		registerFunction( "pagesize", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "pagesize(?1, ?2)" ) );
		registerFunction( "pagesize", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "pagesize(?1, ?2, ?3)" ) );
		registerFunction(
				"partition_id", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "partition_id(?1, ?2)" )
		);
		registerFunction(
				"partition_id", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "partition_id(?1, ?2, ?3)" )
		);
		registerFunction(
				"partition_name", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "partition_name(?1, ?2)" )
		);
		registerFunction(
				"partition_name", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "partition_name(?1, ?2, ?3)" )
		);
		registerFunction( "patindex", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "patindex" ) );
		registerFunction( "proc_role", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "proc_role" ) );
		registerFunction( "role_name", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "role_name" ) );
		// check return type
		registerFunction( "row_count", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "row_count" ) );
		registerFunction( "rand2", new SQLFunctionTemplate( StandardSpiBasicTypes.DOUBLE, "rand2(?1)" ) );
		registerFunction( "rand2", new SQLFunctionTemplate( StandardSpiBasicTypes.DOUBLE, "rand2" ) );
		registerFunction( "replicate", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "replicate(?1,?2)" ) );
		registerFunction( "role_contain", new SQLFunctionTemplate( StandardSpiBasicTypes.BOOLEAN, "role_contain" ) );
		registerFunction( "role_id", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "role_id" ) );
		registerFunction( "reserved_pages", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "reserved_pages" ) );
		registerFunction( "right", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "right" ) );
		registerFunction( "show_role", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "show_role" ) );
		registerFunction(
				"show_sec_services", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "show_sec_services" )
		);
		registerFunction( "sortkey", new VarArgsSQLFunction( StandardSpiBasicTypes.BINARY, "sortkey(", ",", ")" ) );
		registerFunction( "soundex", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "sounded" ) );
		registerFunction( "stddev", new SQLFunctionTemplate( StandardSpiBasicTypes.DOUBLE, "stddev" ) );
		registerFunction( "stddev_pop", new SQLFunctionTemplate( StandardSpiBasicTypes.DOUBLE, "stddev_pop" ) );
		registerFunction( "stddev_samp", new SQLFunctionTemplate( StandardSpiBasicTypes.DOUBLE, "stddev_samp" ) );
		registerFunction( "stuff", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "stuff" ) );
		registerFunction( "substring", new VarArgsSQLFunction( StandardSpiBasicTypes.STRING, "substring(", ",", ")" ) );
		registerFunction( "suser_id", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "suser_id" ) );
		registerFunction( "suser_name", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "suser_name" ) );
		registerFunction( "tempdb_id", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "tempdb_id" ) );
		registerFunction( "textvalid", new SQLFunctionTemplate( StandardSpiBasicTypes.BOOLEAN, "textvalid" ) );
		registerFunction( "to_unichar", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "to_unichar(?1)" ) );
		registerFunction(
				"tran_dumptable_status",
				new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "ran_dumptable_status(?1)" )
		);
		registerFunction( "uhighsurr", new SQLFunctionTemplate( StandardSpiBasicTypes.BOOLEAN, "uhighsurr" ) );
		registerFunction( "ulowsurr", new SQLFunctionTemplate( StandardSpiBasicTypes.BOOLEAN, "ulowsurr" ) );
		registerFunction( "uscalar", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "uscalar" ) );
		registerFunction( "used_pages", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "used_pages" ) );
		registerFunction( "user_id", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "user_id" ) );
		registerFunction( "user_name", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "user_name" ) );
		registerFunction( "valid_name", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "valid_name" ) );
		registerFunction( "valid_user", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "valid_user" ) );
		registerFunction( "variance", new SQLFunctionTemplate( StandardSpiBasicTypes.DOUBLE, "variance" ) );
		registerFunction( "var_pop", new SQLFunctionTemplate( StandardSpiBasicTypes.DOUBLE, "var_pop" ) );
		registerFunction( "var_samp", new SQLFunctionTemplate( StandardSpiBasicTypes.DOUBLE, "var_samp" ) );
		registerFunction( "sysdate", new NoArgSQLFunction( "getdate", StandardSpiBasicTypes.TIMESTAMP) );

		registerSybaseKeywords();
	}

	private void registerSybaseKeywords() {
		registerKeyword( "add" );
		registerKeyword( "all" );
		registerKeyword( "alter" );
		registerKeyword( "and" );
		registerKeyword( "any" );
		registerKeyword( "arith_overflow" );
		registerKeyword( "as" );
		registerKeyword( "asc" );
		registerKeyword( "at" );
		registerKeyword( "authorization" );
		registerKeyword( "avg" );
		registerKeyword( "begin" );
		registerKeyword( "between" );
		registerKeyword( "break" );
		registerKeyword( "browse" );
		registerKeyword( "bulk" );
		registerKeyword( "by" );
		registerKeyword( "cascade" );
		registerKeyword( "case" );
		registerKeyword( "char_convert" );
		registerKeyword( "check" );
		registerKeyword( "checkpoint" );
		registerKeyword( "close" );
		registerKeyword( "clustered" );
		registerKeyword( "coalesce" );
		registerKeyword( "commit" );
		registerKeyword( "compute" );
		registerKeyword( "confirm" );
		registerKeyword( "connect" );
		registerKeyword( "constraint" );
		registerKeyword( "continue" );
		registerKeyword( "controlrow" );
		registerKeyword( "convert" );
		registerKeyword( "count" );
		registerKeyword( "count_big" );
		registerKeyword( "create" );
		registerKeyword( "current" );
		registerKeyword( "cursor" );
		registerKeyword( "database" );
		registerKeyword( "dbcc" );
		registerKeyword( "deallocate" );
		registerKeyword( "declare" );
		registerKeyword( "decrypt" );
		registerKeyword( "default" );
		registerKeyword( "delete" );
		registerKeyword( "desc" );
		registerKeyword( "determnistic" );
		registerKeyword( "disk" );
		registerKeyword( "distinct" );
		registerKeyword( "drop" );
		registerKeyword( "dummy" );
		registerKeyword( "dump" );
		registerKeyword( "else" );
		registerKeyword( "encrypt" );
		registerKeyword( "end" );
		registerKeyword( "endtran" );
		registerKeyword( "errlvl" );
		registerKeyword( "errordata" );
		registerKeyword( "errorexit" );
		registerKeyword( "escape" );
		registerKeyword( "except" );
		registerKeyword( "exclusive" );
		registerKeyword( "exec" );
		registerKeyword( "execute" );
		registerKeyword( "exist" );
		registerKeyword( "exit" );
		registerKeyword( "exp_row_size" );
		registerKeyword( "external" );
		registerKeyword( "fetch" );
		registerKeyword( "fillfactor" );
		registerKeyword( "for" );
		registerKeyword( "foreign" );
		registerKeyword( "from" );
		registerKeyword( "goto" );
		registerKeyword( "grant" );
		registerKeyword( "group" );
		registerKeyword( "having" );
		registerKeyword( "holdlock" );
		registerKeyword( "identity" );
		registerKeyword( "identity_gap" );
		registerKeyword( "identity_start" );
		registerKeyword( "if" );
		registerKeyword( "in" );
		registerKeyword( "index" );
		registerKeyword( "inout" );
		registerKeyword( "insensitive" );
		registerKeyword( "insert" );
		registerKeyword( "install" );
		registerKeyword( "intersect" );
		registerKeyword( "into" );
		registerKeyword( "is" );
		registerKeyword( "isolation" );
		registerKeyword( "jar" );
		registerKeyword( "join" );
		registerKeyword( "key" );
		registerKeyword( "kill" );
		registerKeyword( "level" );
		registerKeyword( "like" );
		registerKeyword( "lineno" );
		registerKeyword( "load" );
		registerKeyword( "lock" );
		registerKeyword( "materialized" );
		registerKeyword( "max" );
		registerKeyword( "max_rows_per_page" );
		registerKeyword( "min" );
		registerKeyword( "mirror" );
		registerKeyword( "mirrorexit" );
		registerKeyword( "modify" );
		registerKeyword( "national" );
		registerKeyword( "new" );
		registerKeyword( "noholdlock" );
		registerKeyword( "nonclustered" );
		registerKeyword( "nonscrollable" );
		registerKeyword( "non_sensitive" );
		registerKeyword( "not" );
		registerKeyword( "null" );
		registerKeyword( "nullif" );
		registerKeyword( "numeric_truncation" );
		registerKeyword( "of" );
		registerKeyword( "off" );
		registerKeyword( "offsets" );
		registerKeyword( "on" );
		registerKeyword( "once" );
		registerKeyword( "online" );
		registerKeyword( "only" );
		registerKeyword( "open" );
		registerKeyword( "option" );
		registerKeyword( "or" );
		registerKeyword( "order" );
		registerKeyword( "out" );
		registerKeyword( "output" );
		registerKeyword( "over" );
		registerKeyword( "artition" );
		registerKeyword( "perm" );
		registerKeyword( "permanent" );
		registerKeyword( "plan" );
		registerKeyword( "prepare" );
		registerKeyword( "primary" );
		registerKeyword( "print" );
		registerKeyword( "privileges" );
		registerKeyword( "proc" );
		registerKeyword( "procedure" );
		registerKeyword( "processexit" );
		registerKeyword( "proxy_table" );
		registerKeyword( "public" );
		registerKeyword( "quiesce" );
		registerKeyword( "raiserror" );
		registerKeyword( "read" );
		registerKeyword( "readpast" );
		registerKeyword( "readtext" );
		registerKeyword( "reconfigure" );
		registerKeyword( "references" );
		registerKeyword( "remove" );
		registerKeyword( "reorg" );
		registerKeyword( "replace" );
		registerKeyword( "replication" );
		registerKeyword( "reservepagegap" );
		registerKeyword( "return" );
		registerKeyword( "returns" );
		registerKeyword( "revoke" );
		registerKeyword( "role" );
		registerKeyword( "rollback" );
		registerKeyword( "rowcount" );
		registerKeyword( "rows" );
		registerKeyword( "rule" );
		registerKeyword( "save" );
		registerKeyword( "schema" );
		registerKeyword( "scroll" );
		registerKeyword( "scrollable" );
		registerKeyword( "select" );
		registerKeyword( "semi_sensitive" );
		registerKeyword( "set" );
		registerKeyword( "setuser" );
		registerKeyword( "shared" );
		registerKeyword( "shutdown" );
		registerKeyword( "some" );
		registerKeyword( "statistics" );
		registerKeyword( "stringsize" );
		registerKeyword( "stripe" );
		registerKeyword( "sum" );
		registerKeyword( "syb_identity" );
		registerKeyword( "syb_restree" );
		registerKeyword( "syb_terminate" );
		registerKeyword( "top" );
		registerKeyword( "table" );
		registerKeyword( "temp" );
		registerKeyword( "temporary" );
		registerKeyword( "textsize" );
		registerKeyword( "to" );
		registerKeyword( "tracefile" );
		registerKeyword( "tran" );
		registerKeyword( "transaction" );
		registerKeyword( "trigger" );
		registerKeyword( "truncate" );
		registerKeyword( "tsequal" );
		registerKeyword( "union" );
		registerKeyword( "unique" );
		registerKeyword( "unpartition" );
		registerKeyword( "update" );
		registerKeyword( "use" );
		registerKeyword( "user" );
		registerKeyword( "user_option" );
		registerKeyword( "using" );
		registerKeyword( "values" );
		registerKeyword( "varying" );
		registerKeyword( "view" );
		registerKeyword( "waitfor" );
		registerKeyword( "when" );
		registerKeyword( "where" );
		registerKeyword( "while" );
		registerKeyword( "with" );
		registerKeyword( "work" );
		registerKeyword( "writetext" );
		registerKeyword( "xmlextract" );
		registerKeyword( "xmlparse" );
		registerKeyword( "xmltest" );
		registerKeyword( "xmlvalidate" );
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public int getMaxAliasLength() {
		return 30;
	}

	/**
	 * By default, Sybase string comparisons are case-insensitive.
	 * <p/>
	 * If the DB is configured to be case-sensitive, then this return
	 * value will be incorrect.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	@Override
	public String getCurrentTimestampSQLFunctionName() {
		return "getdate()";
	}

	/**
	 * Actually Sybase does not support LOB locators at al.
	 *
	 * @return false.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return false;
	}

	@Override
	public String getCrossJoinSeparator() {
		return ", ";
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		return sqlCode == Types.BOOLEAN ? TinyIntSqlDescriptor.INSTANCE : super.getSqlTypeDescriptorOverride( sqlCode );
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}
}
