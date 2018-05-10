/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryTimeoutException;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.SQLServer2005LimitHandler;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.type.StandardBasicTypes;

/**
 * A dialect for Microsoft SQL 2005. (HHH-3936 fix)
 *
 * @author Yoryos Valotasios
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings("deprecation")
public class SQLServer2005Dialect extends SQLServerDialect {
	private static final int MAX_LENGTH = 8000;

	/**
	 * Constructs a SQLServer2005Dialect
	 */
	public SQLServer2005Dialect() {
		// HHH-3965 fix
		// As per http://www.sql-server-helper.com/faq/sql-server-2005-varchar-max-p01.aspx
		// use varchar(max) and varbinary(max) instead of TEXT and IMAGE types
		registerColumnType( Types.BLOB, "varbinary(MAX)" );
		registerColumnType( Types.VARBINARY, "varbinary(MAX)" );
		registerColumnType( Types.VARBINARY, MAX_LENGTH, "varbinary($l)" );
		registerColumnType( Types.LONGVARBINARY, "varbinary(MAX)" );

		registerColumnType( Types.CLOB, "varchar(MAX)" );
		registerColumnType( Types.LONGVARCHAR, "varchar(MAX)" );
		registerColumnType( Types.VARCHAR, "varchar(MAX)" );
		registerColumnType( Types.VARCHAR, MAX_LENGTH, "varchar($l)" );

		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.BIT, "bit" );
		
		// HHH-8435 fix
		registerColumnType( Types.NCLOB, "nvarchar(MAX)" );

		registerFunction( "row_number", new NoArgSQLFunction( "row_number", StandardBasicTypes.INTEGER, true ) );
	}

	@Override
	protected LimitHandler getDefaultLimitHandler() {
		return new SQLServer2005LimitHandler();
	}

	@Override
	public String appendLockHint(LockOptions lockOptions, String tableName) {

		LockMode lockMode = lockOptions.getAliasSpecificLockMode( tableName );
		if(lockMode == null) {
			lockMode = lockOptions.getLockMode();
		}

		final String writeLockStr = lockOptions.getTimeOut() == LockOptions.SKIP_LOCKED ? "updlock" : "updlock, holdlock";
		final String readLockStr = lockOptions.getTimeOut() == LockOptions.SKIP_LOCKED ? "updlock" : "holdlock";

		final String noWaitStr = lockOptions.getTimeOut() == LockOptions.NO_WAIT ? ", nowait" : "";
		final String skipLockStr = lockOptions.getTimeOut() == LockOptions.SKIP_LOCKED ? ", readpast" : "";

		switch ( lockMode ) {
			case UPGRADE:
			case PESSIMISTIC_WRITE:
			case WRITE: {
				return tableName + " with (" + writeLockStr + ", rowlock" + noWaitStr + skipLockStr + ")";
			}
			case PESSIMISTIC_READ: {
				return tableName + " with (" + readLockStr + ", rowlock" + noWaitStr + skipLockStr + ")";
			}
			case UPGRADE_SKIPLOCKED:
				return tableName + " with (updlock, rowlock, readpast" + noWaitStr + ")";
			case UPGRADE_NOWAIT:
				return tableName + " with (updlock, holdlock, rowlock, nowait)";
			default: {
				return tableName;
			}
		}
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
				if ( "HY008".equals( sqlState ) ) {
					throw new QueryTimeoutException( message, sqlException, sql );
				}
				if (1222 == errorCode ) {
					throw new LockTimeoutException( message, sqlException, sql );
				}
				return null;
			}
		};
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return true;
	}

	@Override
	public boolean supportsSkipLocked() {
		return true;
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}
}
