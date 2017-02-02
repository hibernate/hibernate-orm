/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.util.Locale;

import org.hibernate.engine.spi.RowSelection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Tests the Limit/Offset handler for {@link SQLServer2012Dialect}.
 *
 * @author Chris Cranford
 */
public class SQLServer2012DialectTestCase extends BaseUnitTestCase {
	private SQLServer2012Dialect dialect;

	@Before
	public void setup() {
		dialect = new SQLServer2012Dialect();
	}

	@After
	public void tearDown() {
		dialect = null;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8768")
	public void testGetLimitStringMaxRowsOnly() {
		final String input = "select distinct f1 as f53245 from table846752 order by f234, f67 desc";
		assertEquals(
				input + " offset 0 rows fetch next ? rows only",
				dialect.getLimitHandler().processSql( input, toRowSelection( 0, 10 ) ).toLowerCase( Locale.ROOT )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8768")
	public void testGetLimitStringWithOffsetAndMaxRows() {
		final String input = "select distinct f1 as f53245 from table846752 order by f234, f67 desc";
		assertEquals(
				input + " offset ? rows fetch next ? rows only",
				dialect.getLimitHandler().processSql( input, toRowSelection( 5, 25 ) ).toLowerCase( Locale.ROOT )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8768")
	public void testGetLimitStringMaxRowsOnlyNoOrderBy() {
		// this test defaults back to validating result matches that from SQLServer2005LimitHandler
		// See SQLServer2012LimitHandler for why this falls back
		final String input = "select f1 from table";
		assertEquals(
				"select top(?) f1 from table",
				dialect.getLimitHandler().processSql( input, toRowSelection( 0, 10 ) ).toLowerCase( Locale.ROOT )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8768")
	public void testGetLimitStringWithOffsetAndMaxRowsNoOrderBy() {
		// this test defaults back to validating result matches that from SQLServer2005LimitHandler
		// See SQLServer2012LimitHandler for why this falls back
		final String input = "select f1 from table";
		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __hibernate_row_nr__ " +
						"from ( select f1 as page0_ from table ) inner_query ) select page0_ from query where " +
						"__hibernate_row_nr__ >= ? and __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( input, toRowSelection( 5, 10 ) ).toLowerCase( Locale.ROOT )
		);
	}

	private RowSelection toRowSelection(int firstRow, int maxRows) {
		final RowSelection selection = new RowSelection();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
