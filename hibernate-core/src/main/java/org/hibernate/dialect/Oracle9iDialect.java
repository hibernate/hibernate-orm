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

import org.hibernate.LockOptions;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.sql.ANSICaseFragment;
import org.hibernate.sql.CaseFragment;

/**
 * A dialect for Oracle 9i databases.
 * <p/>
 * Specifies to not use "ANSI join syntax" because 9i does not seem to properly handle it in all cases.
 *
 * @author Steve Ebersole
 */
public class Oracle9iDialect extends Oracle8iDialect {
	@Override
	protected void registerCharacterTypeMappings() {
		registerColumnType( Types.CHAR, "char(1 char)" );
		registerColumnType( Types.VARCHAR, 4000, "varchar2($l char)" );
		registerColumnType( Types.VARCHAR, "long" );
	}

	@Override
	protected void registerDateTimeTypeMappings() {
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "date" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
	}

	@Override
	public CaseFragment createCaseFragment() {
		// Oracle did add support for ANSI CASE statements in 9i
		return new ANSICaseFragment();
	}

	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
        return new AbstractLimitHandler(sql, selection) {
        	@Override
        	public String getProcessedSql() {
        		boolean hasOffset = LimitHelper.hasFirstRow(selection);
        		sql = sql.trim();
        		String forUpdateClause = null;
        		boolean isForUpdate = false;
        		final int forUpdateIndex = sql.toLowerCase().lastIndexOf( "for update") ;
        		if ( forUpdateIndex > -1 ) {
        			// save 'for update ...' and then remove it
        			forUpdateClause = sql.substring( forUpdateIndex );
        			sql = sql.substring( 0, forUpdateIndex-1 );
        			isForUpdate = true;
        		}

        		final StringBuilder pagingSelect = new StringBuilder( sql.length() + 100 );
        		if (hasOffset) {
        			pagingSelect.append( "select * from ( select row_.*, rownum rownum_ from ( " );
        		}
        		else {
        			pagingSelect.append( "select * from ( " );
        		}
        		pagingSelect.append( sql );
        		if (hasOffset) {
        			pagingSelect.append( " ) row_ where rownum <= ?) where rownum_ > ?" );
        		}
        		else {
        			pagingSelect.append( " ) where rownum <= ?" );
        		}

        		if ( isForUpdate ) {
        			pagingSelect.append( " " );
        			pagingSelect.append( forUpdateClause );
        		}

        		return pagingSelect.toString();
        	}

        	@Override
        	public boolean supportsLimit() {
        		return true;
        	}

        	@Override
        	public boolean bindLimitParametersInReverseOrder() {
        		return true;
        	}

        	@Override
        	public boolean useMaxForLimit() {
        		return true;
        	}
        };
    }

	@Override
	public String getSelectClauseNullString(int sqlType) {
		return getBasicSelectClauseNullString( sqlType );
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select systimestamp from dual";
	}

	@Override
	public String getCurrentTimestampSQLFunctionName() {
		// the standard SQL function name is current_timestamp...
		return "current_timestamp";
	}

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return " for update nowait";
		}
		else if ( timeout > 0 ) {
			// convert from milliseconds to seconds
			final float seconds = timeout / 1000.0f;
			timeout = Math.round( seconds );
			return " for update wait " + timeout;
		}
		else {
			return " for update";
		}
	}

	@Override
	public String getReadLockString(int timeout) {
		return getWriteLockString( timeout );
	}

	/**
	 * HHH-4907, I don't know if oracle 8 supports this syntax, so I'd think it is better add this 
	 * method here. Reopen this issue if you found/know 8 supports it.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}	
}
