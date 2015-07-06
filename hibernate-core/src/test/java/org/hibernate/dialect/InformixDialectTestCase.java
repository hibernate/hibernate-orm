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
 * Testing of patched support for Informix boolean type; see HHH-9894
 *
 * @author Greg Jones
 */
@TestForIssue( jiraKey = "HHH-9894" )
public class InformixDialectTestCase extends BaseUnitTestCase {
	private final InformixDialect dialect = new InformixDialect();

	@Test
	public void testToBooleanValueStringTrue() {
		assertEquals( "'t'", dialect.toBooleanValueString( true ) );
	}

	@Test
	public void testToBooleanValueStringFalse() {
		assertEquals( "'f'", dialect.toBooleanValueString( false ) );
	}

}
