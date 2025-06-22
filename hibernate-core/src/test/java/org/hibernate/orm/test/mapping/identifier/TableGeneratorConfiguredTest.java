/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class TableGeneratorConfiguredTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::identifiers-generators-table-persist-example[]
			for (long i = 1; i <= 3; i++) {
				Product product = new Product();
				product.setName(String.format("Product %d", i));
				entityManager.persist(product);
			}
			//end::identifiers-generators-table-persist-example[]
		});
	}

	//tag::identifiers-generators-table-mapping-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue(
			strategy = GenerationType.TABLE,
			generator = "table-generator"
		)
		@TableGenerator(
			name =  "table-generator",
			table = "table_identifier",
			pkColumnName = "table_name",
			valueColumnName = "product_id",
			allocationSize = 5
		)
		private Long id;

		@Column(name = "product_name")
		private String name;

		//Getters and setters are omitted for brevity

	//end::identifiers-generators-table-mapping-example[]

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

	//tag::identifiers-generators-table-mapping-example[]
	}
	//end::identifiers-generators-table-mapping-example[]
}
