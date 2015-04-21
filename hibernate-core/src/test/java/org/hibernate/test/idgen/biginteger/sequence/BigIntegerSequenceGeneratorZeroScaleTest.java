/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
