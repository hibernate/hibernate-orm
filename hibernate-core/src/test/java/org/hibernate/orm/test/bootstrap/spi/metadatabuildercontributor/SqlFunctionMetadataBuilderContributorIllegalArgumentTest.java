/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.metadatabuildercontributor;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@JiraKey(value = "HHH-12589")
public class SqlFunctionMetadataBuilderContributorIllegalArgumentTest
		extends AbstractSqlFunctionMetadataBuilderContributorTest {

	@Override
	protected Object matadataBuilderContributor() {
		return new Object();
	}

	@Override
	public EntityManagerFactory produceEntityManagerFactory() {
		EntityManagerFactory entityManagerFactory = null;
		try {
			entityManagerFactory = super.produceEntityManagerFactory();
			fail( "Should throw exception!" );
		}
		catch (IllegalArgumentException e) {
			assertThat( e.getMessage() ).startsWith(
					"The provided hibernate.metadata_builder_contributor setting value" );
		}
		return entityManagerFactory;
	}

	@Override
	@Test
	public void test() {
		try {
			super.test();
			fail( "Should throw exception!" );
		}
		catch (Exception expected) {
		}
	}
}
