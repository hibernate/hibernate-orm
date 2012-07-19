/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
import org.hibernate.engine.spi.RowSelection;
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
public class SQLServer2005Dialect extends SQLServerDialect {
	private static final int MAX_LENGTH = 8000;

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
		registerColumnType( Types.BOOLEAN, "bit" );


		registerFunction( "row_number", new NoArgSQLFunction( "row_number", StandardBasicTypes.INTEGER, true ) );
	}

	@Override
	public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
		return new SQLServer2005LimitHandler( sql, selection );
	}

	@Override // since SQLServer2005 the nowait hint is supported
	public String appendLockHint(LockOptions lockOptions, String tableName) {
		if ( lockOptions.getLockMode() == LockMode.UPGRADE_NOWAIT ) {
			return tableName + " with (updlock, rowlock, nowait)";
		}
		LockMode mode = lockOptions.getLockMode();
		boolean isNoWait = lockOptions.getTimeOut() == LockOptions.NO_WAIT;
		String noWaitStr = isNoWait? ", nowait" :"";
		switch ( mode ) {
			case UPGRADE_NOWAIT:
				 return tableName + " with (updlock, rowlock, nowait)";
			case UPGRADE:
			case PESSIMISTIC_WRITE:
			case WRITE:
				return tableName + " with (updlock, rowlock"+noWaitStr+" )";
			case PESSIMISTIC_READ:
				return tableName + " with (holdlock, rowlock"+noWaitStr+" )";
			default:
				return tableName;
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
}
