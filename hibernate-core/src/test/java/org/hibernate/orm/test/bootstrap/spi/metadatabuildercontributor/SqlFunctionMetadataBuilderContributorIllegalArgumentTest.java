/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.metadatabuildercontributor;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@JiraKey( value = "HHH-12589" )
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
