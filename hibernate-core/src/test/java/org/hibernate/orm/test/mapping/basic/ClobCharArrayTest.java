/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = ClobCharArrayTest.Product.class)
public class ClobCharArrayTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		Integer productId = scope.fromTransaction(
				(em) -> {
					final Product product = new Product();
					product.setId(1);
					product.setName("Mobile phone");
					product.setWarranty("My product warranty".toCharArray());

					em.persist(product);
					return product.getId();
				}
		);

		scope.inTransaction(
				(em) -> {
					final Product product = em.find(Product.class, productId);
					assertArrayEquals("My product warranty".toCharArray(), product.getWarranty());
				}
		);
	}

	@AfterEach
	public void dropData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	//tag::basic-clob-char-array-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String name;

		@Lob
		private char[] warranty;

		//Getters and setters are omitted for brevity

	//end::basic-clob-char-array-example[]
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

		//tag::basic-clob-char-array-example[]
	}
	//end::basic-clob-char-array-example[]
}
