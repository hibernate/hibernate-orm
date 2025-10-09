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

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;


/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {PooledOptimizerTest.Product.class})
public class PooledOptimizerTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::identifiers-generators-pooled-lo-optimizer-persist-example[]
			for (long i = 1; i <= 5; i++) {
				if(i % 3 == 0) {
					entityManager.flush();
				}
				Product product = new Product();
				product.setName(String.format("Product %d", i));
				product.setNumber(String.format("P_100_%d", i));
				entityManager.persist(product);
			}
			//end::identifiers-generators-pooled-lo-optimizer-persist-example[]
		});
	}

	//tag::identifiers-generators-pooled-lo-optimizer-mapping-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue(
			strategy = GenerationType.SEQUENCE,
			generator = "product_generator"
		)
		@GenericGenerator(
			name = "product_generator",
			type = org.hibernate.id.enhanced.SequenceStyleGenerator.class,
			parameters = {
				@Parameter(name = "sequence_name", value = "product_sequence"),
				@Parameter(name = "initial_value", value = "1"),
				@Parameter(name = "increment_size", value = "3"),
				@Parameter(name = "optimizer", value = "pooled-lo")
			}
		)
		private Long id;

		@Column(name = "p_name")
		private String name;

		@Column(name = "p_number")
		private String number;

		//Getters and setters are omitted for brevity

	//end::identifiers-generators-pooled-lo-optimizer-mapping-example[]

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
	//tag::identifiers-generators-pooled-lo-optimizer-mapping-example[]
	}
	//end::identifiers-generators-pooled-lo-optimizer-mapping-example[]
}
