/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.spi.AnsiTrimEmulationFunctionTemplate;
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

		registerSybaseKeywords();
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		registry.registerPattern( "second", "datepart(second, ?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "minute", "datepart(minute, ?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "hour", "datepart(hour, ?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "extract", "datepart(?1, ?3)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "mod", "?1 % ?2", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "bit_length", "datalength(?1) * 8", StandardSpiBasicTypes.INTEGER );
		registry.register(
				"trim", new AnsiTrimEmulationFunctionTemplate(
						AnsiTrimEmulationFunctionTemplate.LTRIM, AnsiTrimEmulationFunctionTemplate.RTRIM, "str_replace"
				)
		);

		registry.registerPattern( "atan2", "atn2(?1, ?2)", StandardSpiBasicTypes.DOUBLE );
		registry.registerPattern( "atn2", "atn2(?1, ?2)", StandardSpiBasicTypes.DOUBLE );

		registry.registerPattern( "biginttohex", "biginttohext(?1)", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "char_length", "char_length(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "charindex", "charindex(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		registry.varArgsBuilder( "coalesce", "coalesce(", ",", ")" ).register();
		registry.registerPattern( "col_length", "col_length(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "col_name", "col_name(?1, ?2)", StandardSpiBasicTypes.STRING );
		// Sybase has created current_date and current_time inplace of getdate()
		registry.registerNoArgs( "current_time", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "current_date", StandardSpiBasicTypes.DATE );


		registry.registerPattern( "data_pages", "data_pages(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "data_pages", "data_pages(?1, ?2, ?3)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "data_pages", "data_pages(?1, ?2, ?3, ?4)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "datalength", "datalength(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "dateadd", "dateadd(?1, ?2, ?3)", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerPattern( "datediff", "datediff(?1, ?2, ?3)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "datepart", "datepart(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "datetime", "datetime", StandardSpiBasicTypes.TIMESTAMP );
		registry.registerPattern( "db_id", "db_id(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "difference", "difference(?1,?2)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "db_name", "db_name(?1)", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "has_role", "has_role(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "hextobigint", "hextobigint(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "hextoint", "hextoint(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "host_id", "host_id", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "host_name", "host_name", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "inttohex", "inttohex(?1)", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "is_quiesced", "is_quiesced(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "is_sec_service_on", "is_sec_service_on(?1)", StandardSpiBasicTypes.BOOLEAN );
		registry.registerPattern( "object_id", "object_id(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "object_name", "object_name(?1)", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "pagesize", "pagesize(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "pagesize", "pagesize(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "pagesize", "pagesize(?1, ?2, ?3)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "partition_id", "partition_id(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "partition_id", "partition_id(?1, ?2, ?3)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "partition_name", "partition_name(?1, ?2)", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "partition_name", "partition_name(?1, ?2, ?3)", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "patindex", "patindex", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "proc_role", "proc_role", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "role_name", "role_name", StandardSpiBasicTypes.STRING );
		// check return type
		registry.registerPattern( "row_count", "row_count", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "rand2", "rand2(?1)", StandardSpiBasicTypes.DOUBLE );
		registry.registerPattern( "rand2", "rand2", StandardSpiBasicTypes.DOUBLE );
		registry.registerPattern( "replicate", "replicate(?1,?2)", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "role_contain", "role_contain", StandardSpiBasicTypes.BOOLEAN );
		registry.registerPattern( "role_id", "role_id", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "reserved_pages", "reserved_pages", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "right", "right", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "show_role", "show_role", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "show_sec_services", "show_sec_services", StandardSpiBasicTypes.STRING );
		registry.registerVarArgs( "sortkey", StandardSpiBasicTypes.BINARY, "sortkey(", ",", ")" );
		registry.registerPattern( "soundex", "sounded", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "stddev", "stddev", StandardSpiBasicTypes.DOUBLE );
		registry.registerPattern( "stddev_pop", "stddev_pop", StandardSpiBasicTypes.DOUBLE );
		registry.registerPattern( "stddev_samp", "stddev_samp", StandardSpiBasicTypes.DOUBLE );
		registry.registerPattern( "stuff", "stuff", StandardSpiBasicTypes.STRING );
		registry.registerVarArgs( "substring", StandardSpiBasicTypes.STRING, "substring(", ",", ")" );
		registry.registerPattern( "suser_id", "suser_id", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "suser_name", "suser_name", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "tempdb_id", "tempdb_id", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "textvalid", "textvalid", StandardSpiBasicTypes.BOOLEAN );
		registry.registerPattern( "to_unichar", "to_unichar(?1)", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "tran_dumptable_status", "ran_dumptable_status(?1)", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "uhighsurr", "uhighsurr", StandardSpiBasicTypes.BOOLEAN );
		registry.registerPattern( "ulowsurr", "ulowsurr", StandardSpiBasicTypes.BOOLEAN );
		registry.registerPattern( "uscalar", "uscalar", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "used_pages", "used_pages", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "user_id", "user_id", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "user_name", "user_name", StandardSpiBasicTypes.STRING );
		registry.registerPattern( "valid_name", "valid_name", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "valid_user", "valid_user", StandardSpiBasicTypes.INTEGER );
		registry.registerPattern( "variance", "variance", StandardSpiBasicTypes.DOUBLE );
		registry.registerPattern( "var_pop", "var_pop", StandardSpiBasicTypes.DOUBLE );
		registry.registerPattern( "var_samp", "var_samp", StandardSpiBasicTypes.DOUBLE );
		registry.registerNoArgs( "sysdate", StandardSpiBasicTypes.TIMESTAMP );
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
