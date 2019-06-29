/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.SQLException;

import org.hibernate.engine.spi.RowSelection;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Mark Rotteveel
 */
public class Firebird25DialectTestCase extends BaseUnitTestCase {

	protected Firebird25Dialect dialect;

	@Before
	public void initDialect() {
		dialect = createDialect();
	}

	@After
	public void tearDown() {
		dialect = null;
	}

	/**
	 * Test extraction of foreign key constraint from long message format.
	 * <p>
	 * Tests extraction from message {@code violation of FOREIGN KEY constraint "{0}" on table "{1}"}
	 * </p>
	 */
	@Test
	public void testExtractForeignKeyConstraintNameLongMessage() throws Exception {
		final String message = "violation of FOREIGN KEY constraint \"FK_table2_table1\" on table \"table2\"";
		final String expectedConstraintName = "FK_table2_table1";

		checkConstraintExtraction( message, expectedConstraintName );
	}

	/**
	 * Test extraction of foreign key constraint from short message format.
	 * <p>
	 * Tests extraction from message {@code violation of FOREIGN KEY constraint "{0}"}
	 * </p>
	 */
	@Test
	public void testExtractForeignKeyConstraintNameShortMessage() throws Exception {
		final String message = "violation of FOREIGN KEY constraint \"FK_table2_table1\"";
		final String expectedConstraintName = "FK_table2_table1";

		checkConstraintExtraction( message, expectedConstraintName );
	}

	/**
	 * Test extraction of check constraint from long message format.
	 * <p>
	 * Tests extraction from message {@code Operation violates CHECK constraint {0} on view or table {1}}
	 * </p>
	 */
	@Test
	public void testExtractCheckConstraintNameLongMessage() throws Exception {
		final String message = "Operation violates CHECK constraint CHK_xyz_123 on view or table table1";
		final String expectedConstraintName = "CHK_xyz_123";

		checkConstraintExtraction( message, expectedConstraintName );
	}

	/**
	 * Test extraction of check constraint from short message format.
	 * <p>
	 * Tests extraction from message {@code Operation violates CHECK constraint {0} on view or table}
	 * </p>
	 */
	@Test
	public void testExtractCheckConstraintNameShortMessage() throws Exception {
		final String message = "Operation violates CHECK constraint CHK_xyz_123 on view or table";
		final String expectedConstraintName = "CHK_xyz_123";

		checkConstraintExtraction( message, expectedConstraintName );
	}

	@Test
	public void testLimitStringNoOffset() {
		final String query = "select * from rdb$relations";
		final String expectedQuery = "select first ? * from rdb$relations";

		final RowSelection selection = new RowSelection();
		selection.setMaxRows( 10 );
		final String processedQuery = dialect.getLimitHandler().processSql( query, selection );

		assertEquals(expectedQuery, processedQuery);
	}

	@Test
	public void testLimitStringWithOffset() {
		final String query = "select * from rdb$relations";
		final String expectedQuery = "select first ? skip ? * from rdb$relations";

		final RowSelection selection = new RowSelection();
		selection.setMaxRows( 10 );
		selection.setFirstRow( 5 );
		final String processedQuery = dialect.getLimitHandler().processSql( query, selection );

		assertEquals(expectedQuery, processedQuery);
	}

	@Test
	public void testLimitStringCommentBeforeSelect() {
		final String query = "/* a comment */ select * from rdb$relations";
		final String expectedQuery = "/* a comment */ select first ? skip ? * from rdb$relations";

		final RowSelection selection = new RowSelection();
		selection.setMaxRows( 10 );
		selection.setFirstRow( 5 );
		final String processedQuery = dialect.getLimitHandler().processSql( query, selection );

		assertEquals(expectedQuery, processedQuery);
	}

	private void checkConstraintExtraction(final String message, final String expectedConstraintName) {
		String constraintName = dialect.getViolatedConstraintNameExtracter()
				.extractConstraintName( new SQLException( message ) );

		assertEquals(expectedConstraintName, constraintName);
	}

	protected Firebird25Dialect createDialect() {
		return new Firebird25Dialect();
	}
}
