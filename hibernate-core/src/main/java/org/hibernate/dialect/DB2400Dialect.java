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

import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;

/**
 * An SQL dialect for DB2/400.  This class provides support for DB2 Universal Database for iSeries,
 * also known as DB2/400.
 *
 * @author Peter DeGregorio (pdegregorio)
 */
public class DB2400Dialect extends DB2Dialect {
	@Override
	public boolean supportsSequences() {
		return false;
	}

	@Override
	public String getIdentitySelectString() {
		return "select identity_val_local() from sysibm.sysdummy1";
	}

	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
        return new AbstractLimitHandler(sql, selection) {
        	@Override
        	public String getProcessedSql() {
        		if ( LimitHelper.hasFirstRow(selection) ) {
        			throw new UnsupportedOperationException( "query result offset is not supported" );
        		}
    			return sql + " fetch first ? rows only";
        	}

        	@Override
        	public boolean supportsLimit() {
        		return true;
        	}

        	@Override
        	public boolean useMaxForLimit() {
        		return true;
        	}

        	@Override
        	public boolean supportsVariableLimit() {
        		return false;
        	}
        };
    }

	@Override
	public String getForUpdateString() {
		return " for update with rs";
	}
}
