/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.Session;
import org.hibernate.engine.jdbc.proxy.ClobProxy;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {ClobTest.Product.class} )
public class ClobTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		Integer productId = scope.fromTransaction( entityManager -> {
			Session session = entityManager.unwrap(Session.class);

			//tag::basic-clob-persist-example[]
			String warranty = "My product warranty";

			final Product product = new Product();
			product.setId(1);
			product.setName("Mobile phone");

			product.setWarranty(ClobProxy.generateProxy(warranty));

			entityManager.persist(product);
			//end::basic-clob-persist-example[]

			return product.getId();
		});
		scope.inTransaction( entityManager -> {
			try {
				//tag::basic-clob-find-example[]

				Product product = entityManager.find(Product.class, productId);

				try (Reader reader = product.getWarranty().getCharacterStream()) {
					assertEquals("My product warranty", toString(reader));
				}
				//end::basic-clob-find-example[]
			}
			catch (Exception e) {
				fail(e.getMessage());
			}
		});
	}

	private String toString(Reader reader) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(reader);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		int result = bufferedReader.read();

		while(result != -1) {
			byteArrayOutputStream.write((byte) result);
			result = bufferedReader.read();
		}

		return byteArrayOutputStream.toString();
	}


	//tag::basic-clob-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String name;

		@Lob
		private Clob warranty;

		//Getters and setters are omitted for brevity

	//end::basic-clob-example[]
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

		public Clob getWarranty() {
			return warranty;
		}

		public void setWarranty(Clob warranty) {
			this.warranty = warranty;
		}

		//tag::basic-clob-example[]
	}
	//end::basic-clob-example[]
}
