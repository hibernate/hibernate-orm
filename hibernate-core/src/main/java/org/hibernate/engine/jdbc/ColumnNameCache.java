/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache of column-name -> column-index resolutions
 *
 * @author Steve Ebersole
 */
public final class ColumnNameCache {
	private static final float LOAD_FACTOR = .75f;

	private final ConcurrentHashMap<String, Integer> columnNameToIndexCache;

	/**
	 * Constructs a ColumnNameCache
	 *
	 * @param columnCount The number of columns to be cached.
	 */
	public ColumnNameCache(int columnCount) {
		// should *not* need to grow beyond the size of the total number of columns in the rs
		this.columnNameToIndexCache = new ConcurrentHashMap<String, Integer>(
				columnCount + (int)( columnCount * LOAD_FACTOR ) + 1,
				LOAD_FACTOR
		);
	}

	/**
	 * Resolve the column name/alias to its index
	 *
	 * @param columnName The name/alias of the column
	 * @param rs The ResultSet
	 *
	 * @return The index
	 *
	 * @throws SQLException INdicates a problems accessing the underlying JDBC ResultSet
	 */
	public Integer getIndexForColumnName(String columnName, ResultSet rs) throws SQLException {
		final Integer cached = columnNameToIndexCache.get( columnName );
		if ( cached != null ) {
			return cached;
		}
		else {
			final Integer index = Integer.valueOf( rs.findColumn( columnName ) );
			columnNameToIndexCache.put( columnName, index);
			return index;
		}
	}
}
