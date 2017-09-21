/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import static org.junit.Assert.assertEquals;

import java.sql.Types;

import org.hibernate.dialect.TestingDialects.MyDialect1;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for JDBC 4.2 types.
 *
 * @author Philippe Marschall
 */
public class Jdbc42TypesTest extends BaseUnitTestCase {

	private Dialect dialect;

	@Before
	public void setUp() {
		this.dialect = new MyDialect1();
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
				"timestamp",
				actual
				);
	}

}
