/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import static junit.framework.TestCase.assertEquals;

import java.sql.Types;

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
	@TestForIssue(jiraKey = "HHH-11773")
	public void testGetTypeName() {
		// time with time zone
		String actual = dialect.getTypeName( Types.TIME_WITH_TIMEZONE );
		assertEquals(
				"Wrong type. Should be time with time zone",
				"time",
				actual
		);

		// time with time zone
		actual = dialect.getTypeName( Types.TIMESTAMP_WITH_TIMEZONE );
		assertEquals(
				"Wrong type. Should be time with time zone",
				"datetimeoffset",
				actual
				);
	}

}
