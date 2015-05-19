/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
