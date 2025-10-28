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
@Jpa( annotatedClasses = {JpaQuotingTest.Product.class} )
public class JpaQuotingTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::basic-jpa-quoting-persistence-example[]
			Product product = new Product();
			product.setId(1L);
			product.setName("Mobile phone");
			product.setNumber("123-456-7890");
			entityManager.persist(product);
			//end::basic-jpa-quoting-persistence-example[]
		});
	}

	//tag::basic-jpa-quoting-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Long id;

		@Column(name = "\"name\"")
		private String name;

		@Column(name = "\"number\"")
		private String number;

		//Getters and setters are omitted for brevity

	//end::basic-jpa-quoting-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}


		//tag::basic-jpa-quoting-example[]
	}
	//end::basic-jpa-quoting-example[]
}
