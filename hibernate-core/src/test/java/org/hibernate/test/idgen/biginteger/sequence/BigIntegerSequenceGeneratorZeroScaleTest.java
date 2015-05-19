/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.idgen.biginteger.sequence;

import org.junit.Test;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;

/**
 * This test explicitly maps scale="0" for the sequence column. It works around the arithmetic
 * overflow that happens because the generated column cannot accommodate the SQL Server
 * sequence that starts, by default, with the value, -9,223,372,036,854,775,808.
 *
 * @author Gail Badner
 */
@RequiresDialectFeature( value = DialectChecks.SupportsSequences.class )
public class BigIntegerSequenceGeneratorZeroScaleTest extends BigIntegerSequenceGeneratorTest {

	public String[] getMappings() {
		return new String[] { "idgen/biginteger/sequence/ZeroScaleMapping.hbm.xml" };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9250")
	public void testBasics() {
		super.testBasics();
	}

}
