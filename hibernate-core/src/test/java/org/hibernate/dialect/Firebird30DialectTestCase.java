/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.engine.spi.RowSelection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Firebird30DialectTestCase extends Firebird25DialectTestCase {

	@Override
	@Test
	public void testLimitStringNoOffset() {
		final String query = "select * from rdb$relations";
		final String expectedQuery = "select * from rdb$relations fetch first ? rows only";

		final RowSelection selection = new RowSelection();
		selection.setMaxRows( 10 );
		final String processedQuery = dialect.getLimitHandler().processSql( query, selection );

		assertEquals(expectedQuery, processedQuery);
	}

	@Override
	@Test
	public void testLimitStringWithOffset() {
		final String query = "select * from rdb$relations";
		final String expectedQuery = "select * from rdb$relations offset ? rows fetch next ? rows only";

		final RowSelection selection = new RowSelection();
		selection.setMaxRows( 10 );
		selection.setFirstRow( 5 );
		final String processedQuery = dialect.getLimitHandler().processSql( query, selection );

		assertEquals(expectedQuery, processedQuery);
	}

	@Override
	@Test
	public void testLimitStringCommentBeforeSelect() {
		final String query = "/* a comment */ select * from rdb$relations";
		final String expectedQuery = "/* a comment */ select * from rdb$relations offset ? rows fetch next ? rows only";

		final RowSelection selection = new RowSelection();
		selection.setMaxRows( 10 );
		selection.setFirstRow( 5 );
		final String processedQuery = dialect.getLimitHandler().processSql( query, selection );

		assertEquals(expectedQuery, processedQuery);
	}

	@Override
	protected Firebird25Dialect createDialect() {
		return new Firebird30Dialect();
	}
}
