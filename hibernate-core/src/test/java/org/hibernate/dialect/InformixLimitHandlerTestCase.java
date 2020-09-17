/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.engine.spi.RowSelection;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;
import org.hibernate.dialect.pagination.Informix10LimitHandler;
import org.junit.Before;
import static org.junit.Assert.assertEquals;

public class InformixLimitHandlerTestCase extends
		BaseNonConfigCoreFunctionalTestCase {

	private Informix10LimitHandler informixLimitHandler;

	private final String TEST_SQL = "select field from table";

	@Before
	public void setup() {
		informixLimitHandler = Informix10LimitHandler.INSTANCE;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11509")
	public void testCorrectLimit() {
		assertLimitHandlerEquals( "select first 10 field from table", 0, 10 );
		assertLimitHandlerEquals( "select skip 3 first 5 field from table", 3, 5 );
		assertLimitHandlerEquals( "select skip 10 first 5 field from table", 10, 5 );
		assertLimitHandlerEquals( "select skip 55 first 12 field from table", 55, 12 );
	}

	private void assertLimitHandlerEquals(String sql, int firstRow, int maxRows) {
		assertEquals( sql, informixLimitHandler.processSql( TEST_SQL, toRowSelection( firstRow, maxRows ) ) );
	}

	private RowSelection toRowSelection(int firstRow, int maxRows) {
		RowSelection selection = new RowSelection();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
