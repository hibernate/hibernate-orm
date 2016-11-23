/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.RowSelection;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialects;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
public class LegacyLimitHandlerTestCase extends
		BaseNonConfigCoreFunctionalTestCase {

	private final String TEST_SQL = "SELECT field FROM table";

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.USE_LEGACY_LIMIT_HANDLERS, "true" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11194")
	@RequiresDialect(Cache71Dialect.class)
	public void testCache71DialectLegacyLimitHandler() {
		assertLimitHandlerEquals( "SELECT TOP ?  field FROM table" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11194")
	@RequiresDialect(DB2390Dialect.class)
	public void testDB2390DialectLegacyLimitHandler() {
		assertLimitHandlerEquals( "SELECT field FROM table fetch first 6 rows only" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11194")
	@RequiresDialects({ @RequiresDialect(InformixDialect.class), @RequiresDialect(IngresDialect.class)})
	public void testInformixDialectOrIngresDialectLegacyLimitHandler() {
		assertLimitHandlerEquals( "SELECT first 6 field FROM table" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11194")
	@RequiresDialect(RDMSOS2200Dialect.class)
	public void testRDMSOS2200DialectLegacyLimitHandler() {
		assertLimitHandlerEquals( "SELECT field FROM table fetch first 5 rows only" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11194")
	@RequiresDialect(value = SQLServerDialect.class, strictMatching = true)
	public void testSQLServerDialectLegacyLimitHandler() {
		assertLimitHandlerEquals( "SELECT top 6 field FROM table" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11194")
	@RequiresDialect(TimesTenDialect.class)
	public void testTimesTenDialectLegacyLimitHandler() {
		assertLimitHandlerEquals( "SELECT first 6 field FROM table" );
	}

	private void assertLimitHandlerEquals(String sql) {
		assertEquals( sql, getDialect().getLimitHandler().processSql( TEST_SQL, toRowSelection( 1, 5 ) ) );
	}

	private RowSelection toRowSelection(int firstRow, int maxRows) {
		RowSelection selection = new RowSelection();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
