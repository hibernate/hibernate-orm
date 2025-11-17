/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {ExplicitColumnNamingTest.Product.class} )
public class ExplicitColumnNamingTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Product product = new Product();
			product.id = 1;
			entityManager.persist(product);
		});
	}

	//tag::basic-annotation-explicit-column-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String sku;

		private String name;

		@Column(name = "NOTES")
		private String description;
	}
	//end::basic-annotation-explicit-column-example[]
}
