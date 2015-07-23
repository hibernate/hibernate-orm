/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class ResultSetUtil {
	public static List<Map<String,?>> extractResults(ResultSet resultSet) throws SQLException {
		List<Map<String,?>> results = new ArrayList<Map<String, ?>>();

		while ( resultSet.next() ) {
			Map<String,Object> row = new HashMap<String, Object>();
			for ( int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++ ) {
				row.put(
						resultSet.getMetaData().getColumnLabel( i ),
						resultSet.getObject( i )
				);
				results.add( row );
			}
		}

		return results;
	}
}
