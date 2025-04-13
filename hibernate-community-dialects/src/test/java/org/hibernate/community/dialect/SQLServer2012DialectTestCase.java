/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.Locale;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.spi.Limit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Tests the Limit/Offset handler for SQLServerDialect.
 *
 * @author Chris Cranford
 */
public class SQLServer2012DialectTestCase extends BaseUnitTestCase {
	private Dialect dialect;

	@Before
	public void setup() {
		dialect = new SQLServerLegacyDialect( DatabaseVersion.make( 11 ) );
	}

	@After
	public void tearDown() {
		dialect = null;
	}

	@Test
	@JiraKey(value = "HHH-8768")
	public void testGetLimitStringMaxRowsOnly() {
		final String input = "select distinct f1 as f53245 from table846752 order by f234, f67 desc";
		assertEquals(
				input + " offset 0 rows fetch first ? rows only",
				dialect.getLimitHandler().processSql( input, toRowSelection( 0, 10 ) ).toLowerCase( Locale.ROOT )
		);
	}

	@Test
	@JiraKey(value = "HHH-8768")
	public void testGetLimitStringWithOffsetAndMaxRows() {
		final String input = "select distinct f1 as f53245 from table846752 order by f234, f67 desc";
		assertEquals(
				input + " offset ? rows fetch next ? rows only",
				dialect.getLimitHandler().processSql( input, toRowSelection( 5, 25 ) ).toLowerCase( Locale.ROOT )
		);
	}

	@Test
	@JiraKey(value = "HHH-8768")
	public void testGetLimitStringMaxRowsOnlyNoOrderBy() {
		final String input = "select f1 from table";
		assertEquals(
				"select f1 from table order by @@version offset 0 rows fetch first ? rows only",
				dialect.getLimitHandler().processSql( input, toRowSelection( 0, 10 ) ).toLowerCase( Locale.ROOT )
		);
	}

	@Test
	@JiraKey(value = "HHH-8768")
	public void testGetLimitStringWithOffsetAndMaxRowsNoOrderBy() {
		final String input = "select f1 from table";
		assertEquals(
				"select f1 from table order by @@version offset ? rows fetch next ? rows only",
				dialect.getLimitHandler().processSql( input, toRowSelection( 5, 10 ) ).toLowerCase( Locale.ROOT )
		);
	}

	private Limit toRowSelection(int firstRow, int maxRows) {
		final Limit selection = new Limit();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
