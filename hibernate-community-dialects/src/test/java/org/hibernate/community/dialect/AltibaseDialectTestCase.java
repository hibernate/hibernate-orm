/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.community.dialect;

import java.util.Locale;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.spi.Limit;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test of the behavior of the AltibaseDialect utility methods
 *
 * @author Geoffrey Park
 */
public class AltibaseDialectTestCase extends BaseUnitTestCase {
	private Dialect dialect;

	@Before
	public void setUp() {
		dialect = new AltibaseDialect( DatabaseVersion.make( 7, 3 ));
	}

	@After
	public void tearDown() {
		dialect = null;
	}

	@Test
	public void testSupportLimits() {
		assertTrue(dialect.getLimitHandler().supportsLimit());
	}

	@Test
	public void testSelectWithLimitOnly() {
		assertEquals( "select c1, c2 from t1 order by c1, c2 desc limit ?",
					  dialect.getLimitHandler().processSql("select c1, c2 from t1 order by c1, c2 desc",
														   toRowSelection( null, 15 ) ).toLowerCase( Locale.ROOT));
	}

	@Test
	public void testSelectWithOffsetLimit() {
		assertEquals( "select c1, c2 from t1 order by c1, c2 desc limit 1+?,?",
					  dialect.getLimitHandler().processSql("select c1, c2 from t1 order by c1, c2 desc",
														   toRowSelection( 5, 15 ) ).toLowerCase(Locale.ROOT));
	}

	@Test
	public void testSelectWithNoLimit() {
		assertEquals( "select c1, c2 from t1 order by c1, c2 desc",
					  dialect.getLimitHandler().processSql("select c1, c2 from t1 order by c1, c2 desc",
														   null ).toLowerCase(Locale.ROOT));
	}

	private Limit toRowSelection(Integer firstRow, Integer maxRows) {
		Limit selection = new Limit();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
