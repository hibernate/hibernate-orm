/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * @author Gail Badner
 */
class InsertOrderingStatementInspector implements StatementInspector {
	private Map<String, Integer> countBySqlStrings = new HashMap<String, Integer>();

	@Override
	public String inspect(String sql) {
		Integer count = countBySqlStrings.get( sql );
		if ( count == null ) {
			count = 0;
		}
		countBySqlStrings.put( sql, count + 1 );
		return sql;
	}

	int getCount(String sql) {
		Integer count = countBySqlStrings.get( sql );
		return count == null ? 0 : count;
	}

	void clear() {
		countBySqlStrings.clear();
	}

	Map<String, Integer> getCountBySqlStrings() {
		return Collections.unmodifiableMap( countBySqlStrings );
	}
}
