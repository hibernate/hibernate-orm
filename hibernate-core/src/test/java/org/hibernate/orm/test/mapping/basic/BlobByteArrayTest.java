/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author Vlad Mihalcea
 */
public class BlobByteArrayTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Test
	public void test() {
		Integer productId = doInJPA(this::entityManagerFactory, entityManager -> {
			final Product product = new Product();
			product.setId(1);
			product.setName("Mobile phone");
			product.setImage(new byte[] {1, 2, 3});

			entityManager.persist(product);
			return product.getId();
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
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
