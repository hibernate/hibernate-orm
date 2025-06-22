/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(dialectClass = SybaseASEDialect.class)
public class NClobStringTest extends BaseEntityManagerFunctionalTestCase {

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
			product.setWarranty("My product®™ warranty 😍");

			entityManager.persist(product);
			return product.getId();
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			Product product = entityManager.find(Product.class, productId);

			assertEquals("My product®™ warranty 😍", product.getWarranty());
		});
	}

	//tag::basic-nclob-string-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String name;

		@Lob
		@Nationalized
		private String warranty;

		//Getters and setters are omitted for brevity

	//end::basic-nclob-string-example[]
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

		//tag::basic-nclob-string-example[]
	}
	//end::basic-nclob-string-example[]
}
