/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.dialect.function.TransactSQLTrimEmulation;
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
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		queryEngine.getSqmFunctionRegistry().registerPattern( "second", "datepart(second, ?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "minute", "datepart(minute, ?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "hour", "datepart(hour, ?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "extract", "datepart(?1, ?3)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "mod", "?1 % ?2", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "bit_length", "datalength(?1) * 8", StandardSpiBasicTypes.INTEGER );

		//TODO: is this really necessary:
		queryEngine.getSqmFunctionRegistry().register(
				"trim", new TransactSQLTrimEmulation(
						TransactSQLTrimEmulation.LTRIM, TransactSQLTrimEmulation.RTRIM, "str_replace"
				)
		);

		queryEngine.getSqmFunctionRegistry().registerPattern( "atan2", "atn2(?1, ?2)", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerPattern( "atn2", "atn2(?1, ?2)", StandardSpiBasicTypes.DOUBLE );

		queryEngine.getSqmFunctionRegistry().registerPattern( "biginttohex", "biginttohext(?1)", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "char_length", "char_length(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "charindex", "charindex(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "col_length", "col_length(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "col_name", "col_name(?1, ?2)", StandardSpiBasicTypes.STRING );

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "current_date", "current date")
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 0 )
				.register();

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "current_time", "current time")
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setExactArgumentCount( 0 )
				.register();

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "current_timestamp", "current timestamp")
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 0 )
				.register();

		queryEngine.getSqmFunctionRegistry().registerPattern( "data_pages", "data_pages(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "data_pages", "data_pages(?1, ?2, ?3)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "data_pages", "data_pages(?1, ?2, ?3, ?4)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "datalength", "datalength(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "dateadd", "dateadd(?1, ?2, ?3)", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerPattern( "datediff", "datediff(?1, ?2, ?3)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "datepart", "datepart(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "datetime", "datetime", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerPattern( "db_id", "db_id(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "difference", "difference(?1,?2)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "db_name", "db_name(?1)", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "has_role", "has_role(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "hextobigint", "hextobigint(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "hextoint", "hextoint(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "host_id", "host_id", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "host_name", "host_name", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "inttohex", "inttohex(?1)", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "is_quiesced", "is_quiesced(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "is_sec_service_on", "is_sec_service_on(?1)", StandardSpiBasicTypes.BOOLEAN );
		queryEngine.getSqmFunctionRegistry().registerPattern( "object_id", "object_id(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "object_name", "object_name(?1)", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "pagesize", "pagesize(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "pagesize", "pagesize(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "pagesize", "pagesize(?1, ?2, ?3)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "partition_id", "partition_id(?1, ?2)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "partition_id", "partition_id(?1, ?2, ?3)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "partition_name", "partition_name(?1, ?2)", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "partition_name", "partition_name(?1, ?2, ?3)", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "patindex", "patindex", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "proc_role", "proc_role", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "role_name", "role_name", StandardSpiBasicTypes.STRING );
		// check return type
		queryEngine.getSqmFunctionRegistry().registerPattern( "row_count", "row_count", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "rand2", "rand2(?1)", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerPattern( "rand2", "rand2", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerPattern( "replicate", "replicate(?1,?2)", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "role_contain", "role_contain", StandardSpiBasicTypes.BOOLEAN );
		queryEngine.getSqmFunctionRegistry().registerPattern( "role_id", "role_id", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "reserved_pages", "reserved_pages", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "right", "right", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "show_role", "show_role", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "show_sec_services", "show_sec_services", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerVarArgs( "sortkey", StandardSpiBasicTypes.BINARY, "sortkey(", ",", ")" );
		queryEngine.getSqmFunctionRegistry().registerPattern( "soundex", "sounded", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "stddev", "stddev", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerPattern( "stddev_pop", "stddev_pop", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerPattern( "stddev_samp", "stddev_samp", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerPattern( "stuff", "stuff", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "suser_id", "suser_id", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "suser_name", "suser_name", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "tempdb_id", "tempdb_id", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "textvalid", "textvalid", StandardSpiBasicTypes.BOOLEAN );
		queryEngine.getSqmFunctionRegistry().registerPattern( "to_unichar", "to_unichar(?1)", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "tran_dumptable_status", "ran_dumptable_status(?1)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "uhighsurr", "uhighsurr", StandardSpiBasicTypes.BOOLEAN );
		queryEngine.getSqmFunctionRegistry().registerPattern( "ulowsurr", "ulowsurr", StandardSpiBasicTypes.BOOLEAN );
		queryEngine.getSqmFunctionRegistry().registerPattern( "uscalar", "uscalar", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "used_pages", "used_pages", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "user_id", "user_id", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "user_name", "user_name", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "valid_name", "valid_name", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "valid_user", "valid_user", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "variance", "variance", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerPattern( "var_pop", "var_pop", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerPattern( "var_samp", "var_samp", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "sysdate", StandardSpiBasicTypes.TIMESTAMP );
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
