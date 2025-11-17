/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {ExplicitBasicTypeTest.Product.class} )
public class ExplicitBasicTypeTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Product product = new Product();
			product.id = 1;
			entityManager.persist(product);
		});
	}

	//tag::basic-annotation-explicit-example[]
	@Entity(name = "Product")
	public static class Product {

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
