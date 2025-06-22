/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.Session;
import org.hibernate.engine.jdbc.proxy.BlobProxy;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class BlobTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Test
	public void test() {
		Integer productId = doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);

			//tag::basic-blob-persist-example[]
			byte[] image = new byte[] {1, 2, 3};

			final Product product = new Product();
			product.setId(1);
			product.setName("Mobile phone");

			product.setImage(BlobProxy.generateProxy(image));

			entityManager.persist(product);
			//end::basic-blob-persist-example[]

			return product.getId();
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			try {
				//tag::basic-blob-find-example[]

				Product product = entityManager.find(Product.class, productId);

				try (InputStream inputStream = product.getImage().getBinaryStream()) {
					assertArrayEquals(new byte[] {1, 2, 3}, toBytes(inputStream));
				}
				//end::basic-blob-find-example[]
			}
			catch (Exception e) {
				fail(e.getMessage());
			}
		});
	}

	private byte[] toBytes(InputStream inputStream) throws IOException {
		BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		int result = bufferedInputStream.read();

		while(result != -1) {
			byteArrayOutputStream.write((byte) result);
			result = bufferedInputStream.read();
		}

		return byteArrayOutputStream.toByteArray();
	}


	//tag::basic-blob-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String name;

		@Lob
		private Blob image;

		//Getters and setters are omitted for brevity

	//end::basic-blob-example[]
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

		public Blob getImage() {
			return image;
		}

		public void setImage(Blob image) {
			this.image = image;
		}

		//tag::basic-blob-example[]
	}
	//end::basic-blob-example[]
}
