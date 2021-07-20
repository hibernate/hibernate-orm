/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import static org.junit.Assert.assertEquals;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test of the behavior of the SQLServerDialect utility methods
 *
 * @author Philippe Marschall
 */
public class SQLServer2008DialectTestCase extends BaseUnitTestCase {
	private SQLServer2008Dialect dialect;

	@Before
	public void setup() {
		dialect = new SQLServer2008Dialect();
	}

	@After
	public void tearDown() {
		dialect = null;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8768")
	public void testTypeNames() {
		// datetimeoffset
		String actual = dialect.getTypeName( -155 );
		assertEquals(
				"Wrong type. Should be datetimeoffset",
				"datetimeoffset",
				actual
		);

		// datetimeoffset
		actual = dialect.getTypeName( -145 );
		assertEquals(
				"Wrong type. Should be uniqueidentifier",
				"uniqueidentifier",
				actual
				);
	}

}
