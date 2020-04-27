/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.jdbc;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
public class SQLStatementInspector implements StatementInspector {
	private final List<String> sqlQueries = new LinkedList<>();

	public SQLStatementInspector() {
	}

	@Override
	public String inspect(String sql) {
		sqlQueries.add( sql );
		return sql;
	}

	public List<String> getSqlQueries() {
		return sqlQueries;
	}

	public void clear() {
		sqlQueries.clear();
	}

	public void assertExecuted(String expected) {
		assertTrue( sqlQueries.contains( expected ) );
	}

	public void assertExecutedCount(int expected) {
		assertEquals( expected, sqlQueries.size() );
	}

	public void assertNumberOfOccurrenceInQuery(int queryNumber, String toCheck, int expectedNumberOfOccurrences) {
		String query = sqlQueries.get( queryNumber );
		int actual = query.split( toCheck, -1 ).length - 1;
		assertThat( actual, is( expectedNumberOfOccurrences ) );
	}
}
