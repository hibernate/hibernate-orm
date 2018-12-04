/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc;

import java.util.LinkedList;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * todo (6.0) : use ManagedBean to locate SQLStatementInterceptor if specified by Class
 *
 * @author Vlad Mihalcea
 */
public class SQLStatementInterceptor implements StatementInspector {

	private final LinkedList<String> sqlQueries = new LinkedList<>();

	public LinkedList<String> getSqlQueries() {
		return sqlQueries;
	}

	public void clear() {
		sqlQueries.clear();
	}

	public void assertExecuted(String expected) {
		assertTrue(sqlQueries.contains( expected ));
	}

	public void assertExecutedCount(int expected) {
		assertEquals(expected, sqlQueries.size());
	}

	@Override
	public String inspect(String sql) {
		sqlQueries.add( sql );
		return sql;
	}
}
