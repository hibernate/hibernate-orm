/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.query.spi.QueryEngine;

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

		//bigint was added in version 15
		registerColumnType( Types.BIGINT, "bigint" );

		//date / date were introduced in version 12
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );

		registerSybaseKeywords();
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry(queryEngine);

		CommonFunctionFactory.currentDateTimeBigdatetimeParens( queryEngine );
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
	public boolean supportsLockTimeouts() {
		return false;
	}
}
