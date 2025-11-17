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
public class SqlFunctionMetadataBuilderContributorIllegalClassArgumentTest
		extends AbstractSqlFunctionMetadataBuilderContributorTest {

	@Override
	protected Object matadataBuilderContributor() {
		return this.getClass();
	}

	@Override
	public EntityManagerFactory produceEntityManagerFactory() {
		EntityManagerFactory entityManagerFactory = null;
		try {
			entityManagerFactory = super.produceEntityManagerFactory();
			fail( "Should throw exception!" );
		}
		catch (ClassCastException e) {
			assertThat( e.getMessage() )
					.containsAnyOf(
							"cannot be cast to",
							"incompatible with" );
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
