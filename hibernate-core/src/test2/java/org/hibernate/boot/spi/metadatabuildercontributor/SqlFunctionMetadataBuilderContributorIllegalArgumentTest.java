/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi.metadatabuildercontributor;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@TestForIssue( jiraKey = "HHH-12589" )
public class SqlFunctionMetadataBuilderContributorIllegalArgumentTest
		extends AbstractSqlFunctionMetadataBuilderContributorTest {

	@Override
	protected Object matadataBuilderContributor() {
		return new Object();
	}

	@Override
	public void buildEntityManagerFactory() {
		try {
			super.buildEntityManagerFactory();

			fail("Should throw exception!");
		}
		catch (IllegalArgumentException e) {
			assertTrue( e.getMessage().startsWith( "The provided hibernate.metadata_builder_contributor setting value" ) );
		}
	}

	@Override
	public void test() {
		try {
			super.test();

			fail("Should throw exception!");
		}
		catch (Exception expected) {
		}
	}
}
