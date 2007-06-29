// $Id: ColumnNameCache.java 5811 2005-02-20 23:02:37Z oneovthafew $
package org.hibernate.jdbc;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of ColumnNameCache.
 *
 * @author Steve Ebersole
 */
public class ColumnNameCache {

	private final Map columnNameToIndexCache;

	public ColumnNameCache(int columnCount) {
		// should *not* need to grow beyond the size of the total number of columns in the rs
		this.columnNameToIndexCache = new HashMap( columnCount );
	}

	public int getIndexForColumnName(String columnName, ResultSetWrapper rs)throws SQLException {
		Integer cached = ( Integer ) columnNameToIndexCache.get( columnName );
		if ( cached != null ) {
			return cached.intValue();
		}
		else {
			int index = rs.getTarget().findColumn( columnName );
			columnNameToIndexCache.put( columnName, new Integer(index) );
			return index;
		}
	}
}
