/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static jakarta.persistence.GenerationType.SEQUENCE;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {SequenceGeneratorAnnotationNameTest.Product.class})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class SequenceGeneratorAnnotationNameTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			for (long i = 1; i <= 5; i++) {
				if(i % 3 == 0) {
					entityManager.flush();
				}
				Product product = new Product();
				product.setName(String.format("Product %d", i));
				entityManager.persist(product);
			}
		});
	}

	//tag::identifiers-generators-sequence-mapping-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue(
			strategy = SEQUENCE,
			generator = "explicit_product_sequence"
		)
		private Long id;

		@Column(name = "product_name")
		private String name;

		//Getters and setters are omitted for brevity

	//end::identifiers-generators-sequence-mapping-example[]

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

	//tag::identifiers-generators-sequence-mapping-example[]
	}
	//end::identifiers-generators-sequence-mapping-example[]
}
