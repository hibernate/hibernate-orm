/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.JDBCException;
import org.hibernate.LockOptions;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Sybase11JoinFragment;
import org.hibernate.sql.TrimSpec;
import org.hibernate.type.descriptor.sql.spi.SmallIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

/**
 * Dialect for Sybase Adaptive Server Enterprise for
 * Sybase 11.9.2 and above.
 */
public class SybaseASEDialect extends SybaseDialect {

	private final int version;

	int getVersion() {
		return version;
	}

	public SybaseASEDialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10 );
	}

	public SybaseASEDialect() {
		this(1100);
	}

	public SybaseASEDialect(int version) {
		super();
		this.version = version;

		//On Sybase ASE, the 'bit' type cannot be null,
		//and cannot have indexes
		registerColumnType( Types.BOOLEAN, "smallint" );

		if ( getVersion() >= 1500 ) {
			//bigint was added in version 15
			registerColumnType( Types.BIGINT, "bigint" );
		}

		if ( getVersion() >= 1200 ) {
			//date / date were introduced in version 12
			registerColumnType( Types.DATE, "date" );
			registerColumnType( Types.TIME, "time" );
		}

		registerSybaseKeywords();
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		return sqlCode == Types.BOOLEAN
				? SmallIntSqlDescriptor.INSTANCE
				: super.getSqlTypeDescriptorOverride(sqlCode);
	}

	@Override
	public String currentDate() {
		return "current_date()";
	}

	@Override
	public String currentTime() {
		return "current_time()";
	}

	@Override
	public String currentTimestamp() {
		return "current_bigdatetime()";
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
	@SuppressWarnings("deprecation")
	public JoinFragment createOuterJoinFragment() {
		return getVersion() < 1400
				? new Sybase11JoinFragment()
				: super.createOuterJoinFragment();
	}

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
	@SuppressWarnings("deprecation")
	public String getCurrentTimestampSQLFunctionName() {
		return "getdate()";
	}

	@Override
	public String getCrossJoinSeparator() {
		return ", ";
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
	public String getTableTypeString() {
		//HHH-7298 I don't know if this would break something or cause some side affects
		//but it is required to use 'select for update'
		return getVersion() < 1570 ? super.getTableTypeString() : " lock datarows";
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		// Earlier Sybase did not support LOB locators at all
		return getVersion() >= 1570;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public boolean forUpdateOfColumns() {
		return getVersion() >= 1570;
	}

	@Override
	public String getForUpdateString() {
		return getVersion() < 1570 ? super.getForUpdateString() : " for update";
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getVersion() < 1570
				? super.getForUpdateString( aliases )
				: getForUpdateString() + " of " + aliases;
	}

	@Override
	public String appendLockHint(LockOptions mode, String tableName) {
		//TODO: is this really necessary??!
		return getVersion() < 1570 ? super.appendLockHint( mode, tableName ) : tableName;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		//TODO: is this really correct?
		return getVersion() < 1570
				? super.applyLocksToSql( sql, aliasedLockOptions, keyColumnNames )
				: sql + new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString();
	}


	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		if ( getVersion() < 1570 ) {
			return null;
		}

		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
				switch ( sqlState ) {
					case "JZ0TO":
					case "JZ006":
						throw new LockTimeoutException( message, sqlException, sql );
					case "ZZZZZ":
						if (515 == errorCode) {
							// Attempt to insert NULL value into column; column does not allow nulls.
							final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName( sqlException );
							return new ConstraintViolationException( message, sqlException, sql, constraintName );
						}
						break;
				}
				return null;
			}
		};
	}

	@Override
	public LimitHandler getLimitHandler() {
		if ( getVersion() < 1250 ) {
			//support for SELECT TOP was introduced in Sybase ASE 12.5.3
			return super.getLimitHandler();
		}
		return new TopLimitHandler() {
			@Override
			public boolean supportsVariableLimit() {
				return false;
			}
		};
	}

	@Override
	public String trimPattern(TrimSpec specification, char character) {
		return super.trimPattern(specification, character)
				.replace("replace", "str_replace");
	}
}
