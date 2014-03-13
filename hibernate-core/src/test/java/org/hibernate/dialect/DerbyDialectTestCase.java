/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect;

import static org.junit.Assert.assertEquals;

import org.hibernate.engine.spi.RowSelection;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * Testing of patched support for Derby limit and offset queries; see HHH-3972
 *
 * @author Evan Leonard
 */
@TestForIssue( jiraKey = "HHH-3972" )
public class DerbyDialectTestCase extends BaseUnitTestCase {

	private static class LocalDerbyDialect extends DerbyDialect {
		protected boolean isTenPointFiveReleaseOrNewer() {
			return true; // for test sake :)
		}
	}

	@Test
	public void testInsertLimitClause() {
		final int limit = 50;
		final String input = "select * from tablename t where t.cat = 5";
		final String expected = "select * from tablename t where t.cat = 5 fetch first ? rows only";

		RowSelection rowSelection = new RowSelection();
		rowSelection.setFirstRow( 0 );
		rowSelection.setMaxRows( limit );
		final String actual = new LocalDerbyDialect().buildLimitHandler( input, rowSelection ).getProcessedSql();
		assertEquals( expected, actual );
	}

	@Test
	public void testInsertLimitWithOffsetClause() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select * from tablename t where t.cat = 5";
		final String expected = "select * from tablename t where t.cat = 5 offset ? rows fetch next ? rows only";

		RowSelection rowSelection = new RowSelection();
		rowSelection.setFirstRow( offset );
		rowSelection.setMaxRows( limit );
		final String actual = new LocalDerbyDialect().buildLimitHandler( input, rowSelection ).getProcessedSql();
		assertEquals( expected, actual );
	}

	@Test
	public void testInsertLimitWithForUpdateClause() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 for update of c11, c13";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 offset ? rows fetch next ? rows only for update of c11, c13";

		RowSelection rowSelection = new RowSelection();
		rowSelection.setFirstRow( offset );
		rowSelection.setMaxRows( limit );
		final String actual = new LocalDerbyDialect().buildLimitHandler( input, rowSelection ).getProcessedSql();
		assertEquals( expected, actual );
	}

	@Test
	public void testInsertLimitWithWithClause() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' with rr";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' offset ? rows fetch next ? rows only with rr";

		RowSelection rowSelection = new RowSelection();
		rowSelection.setFirstRow( offset );
		rowSelection.setMaxRows( limit );
		final String actual = new LocalDerbyDialect().buildLimitHandler( input, rowSelection ).getProcessedSql();
		assertEquals( expected, actual );
	}

	@Test
	public void testInsertLimitWithForUpdateAndWithClauses() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' for update of c11,c13 with rr";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' offset ? rows fetch next ? rows only for update of c11,c13 with rr";

		RowSelection rowSelection = new RowSelection();
		rowSelection.setFirstRow( offset );
		rowSelection.setMaxRows( limit );
		final String actual = new LocalDerbyDialect().buildLimitHandler( input, rowSelection ).getProcessedSql();
		assertEquals( expected, actual );
	}
}
