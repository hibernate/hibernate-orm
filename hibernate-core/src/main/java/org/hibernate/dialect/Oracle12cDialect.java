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

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.SQL2008StandardLimitHandler;
import org.hibernate.engine.spi.RowSelection;

/**
 * An SQL dialect for Oracle 12c.
 * 
 * @author zhouyanming (zhouyanming@gmail.com)
 */
public class Oracle12cDialect extends Oracle10gDialect {

        public Oracle12cDialect() {
                super();
        }
        
        @Override
    	protected void registerDefaultProperties() {
    		super.registerDefaultProperties();
    		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "true" );
    	}
        
        @Override
    	public boolean supportsIdentityColumns() {
    		return true;
    	}

    	@Override
    	public boolean supportsInsertSelectIdentity() {
    		return true;
    	}

    	@Override
    	public String getIdentityColumnString() {
    		return "generated as identity";
    	}

        @Override
        public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
                return new SQL2008StandardLimitHandler(sql, selection);
        }

}