/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class ImplicitBasicTypeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Product product = new Product();
			product.id = 1;
			entityManager.persist(product);
		});
	}

	//tag::basic-annotation-implicit-example[]
	@Entity(name = "Product")
	public class Product {

		@Id
		private Integer id;

		private String sku;

		private String name;

		private String description;
	}
	//end::basic-annotation-implicit-example[]
}
