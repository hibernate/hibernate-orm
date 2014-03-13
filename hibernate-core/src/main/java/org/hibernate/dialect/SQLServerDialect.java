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

import java.sql.Types;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.function.AnsiTrimEmulationFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.SmallIntTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * A dialect for Microsoft SQL Server 2000
 *
 * @author Gavin King
 */
@SuppressWarnings("deprecation")
public class SQLServerDialect extends AbstractTransactSQLDialect {
	private static final int PARAM_LIST_SIZE_LIMIT = 2100;

	/**
	 * Constructs a SQLServerDialect
	 */
	public SQLServerDialect() {
		registerColumnType( Types.VARBINARY, "image" );
		registerColumnType( Types.VARBINARY, 8000, "varbinary($l)" );
		registerColumnType( Types.LONGVARBINARY, "image" );
		registerColumnType( Types.LONGVARCHAR, "text" );
		registerColumnType( Types.BOOLEAN, "bit" );

		registerFunction( "second", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(second, ?1)" ) );
		registerFunction( "minute", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(minute, ?1)" ) );
		registerFunction( "hour", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(hour, ?1)" ) );
		registerFunction( "locate", new StandardSQLFunction( "charindex", StandardBasicTypes.INTEGER ) );

		registerFunction( "extract", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(?1, ?3)" ) );
		registerFunction( "mod", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "?1 % ?2" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datalength(?1) * 8" ) );

		registerFunction( "trim", new AnsiTrimEmulationFunction() );

		registerKeyword( "top" );
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	/**
	 * Use <tt>insert table(...) values(...) select SCOPE_IDENTITY()</tt>
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String appendIdentitySelectToInsert(String insertSQL) {
		return insertSQL + " select scope_identity()";
	}
	
	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
            return new TopLimitHandler(sql, selection, false, false);
    }

	@Override
	public char closeQuote() {
		return ']';
	}

	@Override
	public char openQuote() {
		return '[';
	}

	@Override
	public String appendLockHint(LockOptions lockOptions, String tableName) {
		final LockMode mode = lockOptions.getLockMode();
		switch ( mode ) {
			case UPGRADE:
			case UPGRADE_NOWAIT:
			case PESSIMISTIC_WRITE:
			case WRITE:
				return tableName + " with (updlock, rowlock)";
			case PESSIMISTIC_READ:
				return tableName + " with (holdlock, rowlock)";
			case UPGRADE_SKIPLOCKED:
				return tableName + " with (updlock, rowlock, readpast)";
			default:
				return tableName;
		}
	}


	/**
	 * The current_timestamp is more accurate, but only known to be supported in SQL Server 7.0 and later and
	 * Sybase not known to support it at all
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		// SQL Server (at least up through 2005) does not support defining
		// cascade delete constraints which can circle back to the mutating
		// table
		return false;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		// note: at least my local SQL Server 2005 Express shows this not working...
		return false;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		// here assume SQLServer2005 using snapshot isolation, which does not have this problem
		return false;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		// here assume SQLServer2005 using snapshot isolation, which does not have this problem
		return false;
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		return sqlCode == Types.TINYINT ?
				SmallIntTypeDescriptor.INSTANCE :
				super.getSqlTypeDescriptorOverride( sqlCode );
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}
}

