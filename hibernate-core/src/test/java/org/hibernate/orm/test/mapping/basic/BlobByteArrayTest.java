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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {BlobByteArrayTest.Product.class} )
public class BlobByteArrayTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		Integer productId = scope.fromTransaction( entityManager -> {
			final Product product = new Product();
			product.setId(1);
			product.setName("Mobile phone");
			product.setImage(new byte[] {1, 2, 3});

			entityManager.persist(product);
			return product.getId();
		});
		scope.inTransaction( entityManager -> {
			Product product = entityManager.find(Product.class, productId);

			assertArrayEquals(new byte[] {1, 2, 3}, product.getImage());
		});
	}

	//tag::basic-blob-byte-array-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String name;

		@Lob
		private byte[] image;

		//Getters and setters are omitted for brevity

	//end::basic-blob-byte-array-example[]
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

		public byte[] getImage() {
			return image;
		}

		public void setImage(byte[] image) {
			this.image = image;
		}

		//tag::basic-blob-byte-array-example[]
	}
	//end::basic-blob-byte-array-example[]
}
