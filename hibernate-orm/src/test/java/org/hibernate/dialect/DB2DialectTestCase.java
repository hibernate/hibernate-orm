/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.junit.Test;

import org.hibernate.mapping.Column;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * DB2 dialect related test cases
 *
 * @author Hardy Ferentschik
 */

public class DB2DialectTestCase extends BaseUnitTestCase {
	private final DB2Dialect dialect = new DB2Dialect();

	@Test
	@TestForIssue(jiraKey = "HHH-6866")
	public void testGetDefaultBinaryTypeName() {
		String actual = dialect.getTypeName( Types.BINARY );
		assertEquals(
				"The default column length is 255, but char length on DB2 is limited to 254",
				"varchar($l) for bit data",
				actual
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6866")
	public void testGetExplicitBinaryTypeName() {
		// lower bound
		String actual = dialect.getTypeName( Types.BINARY, 1, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE );
		assertEquals(
				"Wrong binary type",
				"char(1) for bit data",
				actual
		);

		// upper bound
		actual = dialect.getTypeName( Types.BINARY, 254, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE );
		assertEquals(
				"Wrong binary type. 254 is the max length in DB2",
				"char(254) for bit data",
				actual
		);

		// exceeding upper bound
		actual = dialect.getTypeName( Types.BINARY, 255, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE );
		assertEquals(
				"Wrong binary type. Should be varchar for length > 254",
				"varchar(255) for bit data",
				actual
		);
	}
}
