/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.RowId;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = OracleDialect.class)
@Jpa(
		annotatedClasses = RowIdTest.Product.class
)
public class RowIdTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Product product = new Product();
			product.setId(1L);
			product.setName("Mobile phone");
			product.setNumber("123-456-7890");
			entityManager.persist(product);
		});
		scope.inTransaction( entityManager -> {
			//tag::identifiers-rowid-example[]
			Product product = entityManager.find(Product.class, 1L);

			product.setName("Smart phone");
			//end::identifiers-rowid-example[]
		});
	}

	//tag::identifiers-rowid-mapping[]
	@Entity(name = "Product")
	@RowId("ROWID")
	public static class Product {

		@Id
		private Long id;

		@Column(name = "`name`")
		private String name;

		@Column(name = "`number`")
		private String number;

		//Getters and setters are omitted for brevity

	//end::identifiers-rowid-mapping[]

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


		//tag::identifiers-rowid-mapping[]
	}
	//end::identifiers-rowid-mapping[]
}
