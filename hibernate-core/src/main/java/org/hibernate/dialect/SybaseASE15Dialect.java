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
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.TinyIntTypeDescriptor;

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

		registerFunction( "second", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(second, ?1)" ) );
		registerFunction( "minute", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(minute, ?1)" ) );
		registerFunction( "hour", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(hour, ?1)" ) );
		registerFunction( "extract", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(?1, ?3)" ) );
		registerFunction( "mod", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "?1 % ?2" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datalength(?1) * 8" ) );
		registerFunction(
				"trim", new AnsiTrimEmulationFunction(
						AnsiTrimEmulationFunction.LTRIM, AnsiTrimEmulationFunction.RTRIM, "str_replace"
				)
		);

		registerFunction( "atan2", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "atn2(?1, ?2" ) );
		registerFunction( "atn2", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "atn2(?1, ?2" ) );

		registerFunction( "biginttohex", new SQLFunctionTemplate( StandardBasicTypes.STRING, "biginttohext(?1)" ) );
		registerFunction( "char_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "char_length(?1)" ) );
		registerFunction( "charindex", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "charindex(?1, ?2)" ) );
		registerFunction( "coalesce", new VarArgsSQLFunction( "coalesce(", ",", ")" ) );
		registerFunction( "col_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "col_length(?1, ?2)" ) );
		registerFunction( "col_name", new SQLFunctionTemplate( StandardBasicTypes.STRING, "col_name(?1, ?2)" ) );
		// Sybase has created current_date and current_time inplace of getdate()
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardBasicTypes.TIME ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE ) );


		registerFunction( "data_pages", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "data_pages(?1, ?2)" ) );
		registerFunction(
				"data_pages", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "data_pages(?1, ?2, ?3)" )
		);
		registerFunction(
				"data_pages", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "data_pages(?1, ?2, ?3, ?4)" )
		);
		registerFunction( "datalength", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datalength(?1)" ) );
		registerFunction( "dateadd", new SQLFunctionTemplate( StandardBasicTypes.TIMESTAMP, "dateadd" ) );
		registerFunction( "datediff", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datediff" ) );
		registerFunction( "datepart", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart" ) );
		registerFunction( "datetime", new SQLFunctionTemplate( StandardBasicTypes.TIMESTAMP, "datetime" ) );
		registerFunction( "db_id", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "db_id(?1)" ) );
		registerFunction( "difference", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "difference(?1,?2)" ) );
		registerFunction( "db_name", new SQLFunctionTemplate( StandardBasicTypes.STRING, "db_name(?1)" ) );
		registerFunction( "has_role", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "has_role(?1, ?2)" ) );
		registerFunction( "hextobigint", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "hextobigint(?1)" ) );
		registerFunction( "hextoint", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "hextoint(?1)" ) );
		registerFunction( "host_id", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "host_id" ) );
		registerFunction( "host_name", new SQLFunctionTemplate( StandardBasicTypes.STRING, "host_name" ) );
		registerFunction( "inttohex", new SQLFunctionTemplate( StandardBasicTypes.STRING, "inttohex(?1)" ) );
		registerFunction( "is_quiesced", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "is_quiesced(?1)" ) );
		registerFunction(
				"is_sec_service_on", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "is_sec_service_on(?1)" )
		);
		registerFunction( "object_id", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "object_id(?1)" ) );
		registerFunction( "object_name", new SQLFunctionTemplate( StandardBasicTypes.STRING, "object_name(?1)" ) );
		registerFunction( "pagesize", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "pagesize(?1)" ) );
		registerFunction( "pagesize", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "pagesize(?1, ?2)" ) );
		registerFunction( "pagesize", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "pagesize(?1, ?2, ?3)" ) );
		registerFunction(
				"partition_id", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "partition_id(?1, ?2)" )
		);
		registerFunction(
				"partition_id", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "partition_id(?1, ?2, ?3)" )
		);
		registerFunction(
				"partition_name", new SQLFunctionTemplate( StandardBasicTypes.STRING, "partition_name(?1, ?2)" )
		);
		registerFunction(
				"partition_name", new SQLFunctionTemplate( StandardBasicTypes.STRING, "partition_name(?1, ?2, ?3)" )
		);
		registerFunction( "patindex", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "patindex" ) );
		registerFunction( "proc_role", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "proc_role" ) );
		registerFunction( "role_name", new SQLFunctionTemplate( StandardBasicTypes.STRING, "role_name" ) );
		// check return type
		registerFunction( "row_count", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "row_count" ) );
		registerFunction( "rand2", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "rand2(?1)" ) );
		registerFunction( "rand2", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "rand2" ) );
		registerFunction( "replicate", new SQLFunctionTemplate( StandardBasicTypes.STRING, "replicate(?1,?2)" ) );
		registerFunction( "role_contain", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "role_contain" ) );
		registerFunction( "role_id", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "role_id" ) );
		registerFunction( "reserved_pages", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "reserved_pages" ) );
		registerFunction( "right", new SQLFunctionTemplate( StandardBasicTypes.STRING, "right" ) );
		registerFunction( "show_role", new SQLFunctionTemplate( StandardBasicTypes.STRING, "show_role" ) );
		registerFunction(
				"show_sec_services", new SQLFunctionTemplate( StandardBasicTypes.STRING, "show_sec_services" )
		);
		registerFunction( "sortkey", new VarArgsSQLFunction( StandardBasicTypes.BINARY, "sortkey(", ",", ")" ) );
		registerFunction( "soundex", new SQLFunctionTemplate( StandardBasicTypes.STRING, "sounded" ) );
		registerFunction( "stddev", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "stddev" ) );
		registerFunction( "stddev_pop", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "stddev_pop" ) );
		registerFunction( "stddev_samp", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "stddev_samp" ) );
		registerFunction( "stuff", new SQLFunctionTemplate( StandardBasicTypes.STRING, "stuff" ) );
		registerFunction( "substring", new VarArgsSQLFunction( StandardBasicTypes.STRING, "substring(", ",", ")" ) );
		registerFunction( "suser_id", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "suser_id" ) );
		registerFunction( "suser_name", new SQLFunctionTemplate( StandardBasicTypes.STRING, "suser_name" ) );
		registerFunction( "tempdb_id", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "tempdb_id" ) );
		registerFunction( "textvalid", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "textvalid" ) );
		registerFunction( "to_unichar", new SQLFunctionTemplate( StandardBasicTypes.STRING, "to_unichar(?1)" ) );
		registerFunction(
				"tran_dumptable_status",
				new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "ran_dumptable_status(?1)" )
		);
		registerFunction( "uhighsurr", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "uhighsurr" ) );
		registerFunction( "ulowsurr", new SQLFunctionTemplate( StandardBasicTypes.BOOLEAN, "ulowsurr" ) );
		registerFunction( "uscalar", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "uscalar" ) );
		registerFunction( "used_pages", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "used_pages" ) );
		registerFunction( "user_id", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "user_id" ) );
		registerFunction( "user_name", new SQLFunctionTemplate( StandardBasicTypes.STRING, "user_name" ) );
		registerFunction( "valid_name", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "valid_name" ) );
		registerFunction( "valid_user", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "valid_user" ) );
		registerFunction( "variance", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "variance" ) );
		registerFunction( "var_pop", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "var_pop" ) );
		registerFunction( "var_samp", new SQLFunctionTemplate( StandardBasicTypes.DOUBLE, "var_samp" ) );
		registerFunction( "sysdate", new NoArgSQLFunction("getdate", StandardBasicTypes.TIMESTAMP) );

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
		return sqlCode == Types.BOOLEAN ? TinyIntTypeDescriptor.INSTANCE : super.getSqlTypeDescriptorOverride( sqlCode );
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}
}
