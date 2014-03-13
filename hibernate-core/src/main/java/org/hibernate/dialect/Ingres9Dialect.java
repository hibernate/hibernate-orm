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

import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.type.StandardBasicTypes;

/**
 * A SQL dialect for Ingres 9.3 and later versions.
 * <p/>
 * Changes:
 * <ul>
 * <li>Support for the SQL functions current_time, current_timestamp and current_date added</li>
 * <li>Type mapping of <code>Types.TIMESTAMP</code> changed from "timestamp with time zone" to "timestamp(9) with time zone"</li>
 * <li>Improved handling of "SELECT...FOR UPDATE" statements</li>
 * <li>Added support for pooled sequences</li>
 * <li>Added support for SELECT queries with limit and offset</li>
 * <li>Added getIdentitySelectString</li>
 * <li>Modified concatination operator</li>
 * </ul>
 *
 * @author Enrico Schenk
 * @author Raymond Fan
 */
public class Ingres9Dialect extends IngresDialect {
	/**
	 * Constructs a Ingres9Dialect
	 */
	public Ingres9Dialect() {
		super();
		registerDateTimeFunctions();
		registerDateTimeColumnTypes();
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );
	}

	/**
	 * Register functions current_time, current_timestamp, current_date
	 */
	protected void registerDateTimeFunctions() {
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardBasicTypes.TIME, false ) );
		registerFunction(
				"current_timestamp", new NoArgSQLFunction(
				"current_timestamp",
				StandardBasicTypes.TIMESTAMP,
				false
		)
		);
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE, false ) );
	}

	/**
	 * Register column types date, time, timestamp
	 */
	protected void registerDateTimeColumnTypes() {
		registerColumnType( Types.DATE, "ansidate" );
		registerColumnType( Types.TIMESTAMP, "timestamp(9) with time zone" );
	}

	// lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean forUpdateOfColumns() {
		return true;
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getIdentitySelectString() {
		return "select last_identity()";
	}

	@Override
	public String getQuerySequencesString() {
		return "select seq_name from iisequences";
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	// current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp";
	}

	@Override
	public String getCurrentTimestampSQLFunctionName() {
		return "current_timestamp";
	}

	// union subclass support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	// Informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return true;
	}

	// limit/offset support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
        return new AbstractLimitHandler(sql, selection) {
        	@Override
        	public String getProcessedSql() {
        		final StringBuilder soff = new StringBuilder( " offset ?" );
        		final StringBuilder slim = new StringBuilder( " fetch first ? rows only" );
        		final StringBuilder sb = new StringBuilder( sql.length() + soff.length() + slim.length() )
        				.append( sql );
        		if ( LimitHelper.hasFirstRow(selection) ) {
        			sb.append( soff );
        		}
        		if ( LimitHelper.hasMaxRows(selection) ) {
        			sb.append( slim );
        		}
        		return sb.toString();
        	}

        	@Override
        	public boolean supportsLimit() {
        		return true;
        	}

        	@Override
        	public boolean supportsVariableLimit() {
        		return false;
        	}
        };
    }
}
