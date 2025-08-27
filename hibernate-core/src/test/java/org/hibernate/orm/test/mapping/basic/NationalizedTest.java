/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Nationalized;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = NationalizedTest.Product.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnicodeNClob.class)
public class NationalizedTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		Integer productId = scope.fromTransaction( entityManager -> {
			//tag::basic-nationalized-persist-example[]
			final Product product = new Product();
			product.setId(1);
			product.setName("Mobile phone");
			product.setWarranty("My product®™ warranty 😍");

			entityManager.persist(product);
			//end::basic-nationalized-persist-example[]

			return product.getId();
		});
		scope.inTransaction( entityManager -> {
			Product product = entityManager.find(Product.class, productId);

			assertEquals("My product®™ warranty 😍", product.getWarranty());
		});
	}

	//tag::basic-nationalized-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String name;

		@Nationalized
		private String warranty;

		//Getters and setters are omitted for brevity

	//end::basic-nationalized-example[]
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

		public String getWarranty() {
			return warranty;
		}

		public void setWarranty(String warranty) {
			this.warranty = warranty;
		}

		//tag::basic-nationalized-example[]
	}
	//end::basic-nationalized-example[]
}
