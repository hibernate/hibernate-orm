/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * LIMIT clause handler compatible with SQL Server 2012 and later.
 * 
 * @author Ivan Ermolaev
 */
public class SQLServer2012LimitHandler extends AbstractLimitHandler {

	/**
	 * Constructs a SQLServer2012LimitHandler
	 * 
	 * @param sql The SQL
	 * @param selection The row selection options
	 */
	public SQLServer2012LimitHandler(String sql, RowSelection selection) {
		super( sql, selection );
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	/**
	 * Add a LIMIT clause to the given SQL SELECT. Adds following clause to the SQL:
	 * 
	 * <pre>
	 * offset ? rows fetch next ? rows only
	 * </pre>
	 * 
	 * When first row isn't specified offset would be set to {@literal 0}
	 * 
	 * @return A new SQL statement with the LIMIT clause applied.
	 */
	@Override
	public String getProcessedSql() {
		if ( LimitHelper.hasFirstRow( selection ) ) {
			return sql + " offset ? rows fetch next ? rows only";
		}
		else {
			return sql + " offset 0 rows fetch next ? rows only";
		}
	}

}
