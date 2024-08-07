/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.dialect;

import java.util.Locale;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.spi.Limit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Tests the Limit/Offset handler for {@link SQLServerDialect, v11}.
 *
 * @author Chris Cranford
 */
public class SQLServer2012DialectTestCase extends BaseUnitTestCase {
	private SQLServerDialect dialect;

	@Before
	public void setup() {
		dialect = new SQLServerDialect( DatabaseVersion.make( 11 ) );
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
				input + " offset 0 rows fetch first ? rows only",
				dialect.getLimitHandler().processSql( input, toRowSelection( null, 10 ) ).toLowerCase( Locale.ROOT )
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
		final String input = "select f1 from table";
		assertEquals(
				"select f1 from table order by @@version offset 0 rows fetch first ? rows only",
				dialect.getLimitHandler().processSql( input, toRowSelection( null, 10 ) ).toLowerCase( Locale.ROOT )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8768")
	public void testGetLimitStringWithOffsetAndMaxRowsNoOrderBy() {
		final String input = "select f1 from table";
		assertEquals(
				"select f1 from table order by @@version offset ? rows fetch next ? rows only",
				dialect.getLimitHandler().processSql( input, toRowSelection( 5, 10 ) ).toLowerCase( Locale.ROOT )
		);
	}

	private Limit toRowSelection(Integer firstRow, Integer maxRows) {
		final Limit selection = new Limit();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
