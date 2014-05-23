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

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for Firebird.
 *
 * @author Reha CENANI
 */
public class FirebirdDialect extends InterbaseDialect {
	
	public FirebirdDialect() {
		super();
		registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
	}
	
	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop generator " + sequenceName;
	}

	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
        return new AbstractLimitHandler(sql, selection) {
        	@Override
        	public String getProcessedSql() {
        		boolean hasOffset = LimitHelper.hasFirstRow(selection);
        		return new StringBuilder( sql.length() + 20 )
				.append( sql )
				.insert( 6, hasOffset ? " first ? skip ?" : " first ?" )
				.toString();
        	}

        	@Override
        	public boolean supportsLimit() {
        		return true;
        	}

        	@Override
        	public boolean bindLimitParametersFirst() {
        		return true;
        	}

        	@Override
        	public boolean bindLimitParametersInReverseOrder() {
        		return true;
        	}
        };
    }
}
