/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.Parameter;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.usertype.UserTypeLegacyBridge;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class ExplicitTypeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {

		});
	}

	//tag::basic-type-annotation-example[]
	@Entity(name = "Product")
	public class Product {

		@Id
		private Integer id;

		private String sku;

		@Type(
				value = UserTypeLegacyBridge.class,
				parameters = @Parameter(name = UserTypeLegacyBridge.TYPE_NAME_PARAM_KEY, value = "nstring")
		)
		private String name;

		@Type(
				value = UserTypeLegacyBridge.class,
				parameters = @Parameter(name = UserTypeLegacyBridge.TYPE_NAME_PARAM_KEY, value = "materialized_nclob")
		)
		private String description;
	}
	//end::basic-type-annotation-example[]
}
