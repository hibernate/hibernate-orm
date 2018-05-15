/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi.metadatabuildercontributor;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@TestForIssue( jiraKey = "HHH-12589" )
public class SqlFunctionMetadataBuilderContributorIllegalClassArgumentTest
		extends AbstractSqlFunctionMetadataBuilderContributorTest {

	@Override
	protected Object matadataBuilderContributor() {
		return this.getClass();
	}

	@Override
	public void buildEntityManagerFactory() {
		try {
			super.buildEntityManagerFactory();

			fail("Should throw exception!");
		}
		catch (ClassCastException e) {
			assertTrue( e.getMessage().contains( "cannot be cast to" ) );
			assertTrue( e.getMessage().contains( "org.hibernate.boot.spi.MetadataBuilderContributor" ) );
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
