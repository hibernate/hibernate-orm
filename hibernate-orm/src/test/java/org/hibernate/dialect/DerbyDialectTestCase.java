/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * Testing of patched support for Derby limit and offset queries; see HHH-3972
 *
 * @author Evan Leonard
 */
public class DerbyDialectTestCase extends BaseUnitTestCase {

	private static class LocalDerbyDialect extends DerbyDialect {
		protected boolean isTenPointFiveReleaseOrNewer() {
			return true; // for test sake :)
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3972" )
	public void testInsertLimitClause() {
		final int limit = 50;
		final String input = "select * from tablename t where t.cat = 5";
		final String expected = "select * from tablename t where t.cat = 5 fetch first " + limit + " rows only";

		final String actual = new LocalDerbyDialect().getLimitString( input, 0, limit );
		assertEquals( expected, actual );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3972" )
	public void testInsertLimitWithOffsetClause() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select * from tablename t where t.cat = 5";
		final String expected = "select * from tablename t where t.cat = 5 offset " + offset + " rows fetch next " + limit + " rows only";

		final String actual = new LocalDerbyDialect().getLimitString( input, offset, limit );
		assertEquals( expected, actual );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3972" )
	public void testInsertLimitWithForUpdateClause() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 for update of c11, c13";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 offset " + offset
				+ " rows fetch next " + limit + " rows only for update of c11, c13";

		final String actual = new LocalDerbyDialect().getLimitString( input, offset, limit );
		assertEquals( expected, actual );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3972" )
	public void testInsertLimitWithWithClause() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' with rr";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' offset " + offset
				+ " rows fetch next " + limit + " rows only with rr";

		final String actual = new LocalDerbyDialect().getLimitString( input, offset, limit );
		assertEquals( expected, actual );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3972" )
	public void testInsertLimitWithForUpdateAndWithClauses() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' for update of c11,c13 with rr";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' offset " + offset
				+ " rows fetch next " + limit + " rows only for update of c11,c13 with rr";

		final String actual = new LocalDerbyDialect().getLimitString( input, offset, limit );
		assertEquals( expected, actual );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10238")
	public void testDefaultMultiTableBulkIdStrategyIsLocal() {
		MultiTableBulkIdStrategy actual = new LocalDerbyDialect().getDefaultMultiTableBulkIdStrategy();
		assertThat(actual, is(instanceOf(LocalTemporaryTableBulkIdStrategy.class)));
	}
}
