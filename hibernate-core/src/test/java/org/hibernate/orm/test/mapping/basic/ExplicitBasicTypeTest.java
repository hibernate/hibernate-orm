/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class ExplicitBasicTypeTest extends BaseEntityManagerFunctionalTestCase {

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

	//tag::basic-annotation-explicit-example[]
	@Entity(name = "Product")
	public class Product {

		@Id
		@Basic
		private Integer id;

		@Basic
		private String sku;

		@Basic
		private String name;

		@Basic
		private String description;
	}
	//end::basic-annotation-explicit-example[]
}
