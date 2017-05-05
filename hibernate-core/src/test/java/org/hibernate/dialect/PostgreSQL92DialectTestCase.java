/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test case for PostgreSQL 9.2 specific things.
 *
 * @author Christoph Dreis
 */
@TestForIssue( jiraKey = "HHH-11647")
public class PostgreSQL92DialectTestCase extends BaseUnitTestCase {

	/**
	 * Tests that getAlterTableString() will make use of IF EXISTS syntax
	 */
	@Test
	@TestForIssue( jiraKey = "HHH-11647" )
	public void testGetAlterTableString() {
		PostgreSQL92Dialect dialect = new PostgreSQL92Dialect();

		assertEquals("alter table if exists table_name", dialect.getAlterTableString( "table_name" ));
	}

}
