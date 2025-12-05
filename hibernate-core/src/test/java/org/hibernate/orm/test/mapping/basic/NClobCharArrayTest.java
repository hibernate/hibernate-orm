/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.annotations.Nationalized;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {NClobCharArrayTest.Product.class} )
public class NClobCharArrayTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		Integer productId = scope.fromTransaction( entityManager -> {
			final Product product = new Product();
			product.setId(1);
			product.setName("Mobile phone");
			product.setWarranty("My product warranty".toCharArray());

			entityManager.persist(product);
			return product.getId();
		});
		scope.inTransaction( entityManager -> {
			Product product = entityManager.find(Product.class, productId);

			assertArrayEquals("My product warranty".toCharArray(), product.getWarranty());
		});
	}

	//tag::basic-nclob-char-array-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String name;

		@Lob
		@Nationalized
		private char[] warranty;

		//Getters and setters are omitted for brevity

	//end::basic-nclob-char-array-example[]
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public char[] getWarranty() {
			return warranty;
		}

		public void setWarranty(char[] warranty) {
			this.warranty = warranty;
		}

		//tag::basic-nclob-char-array-example[]
	}
	//end::basic-nclob-char-array-example[]
}
