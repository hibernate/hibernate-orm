/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				NoDynamicUpdateTest.Product.class
		}
)
public class NoDynamicUpdateTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			//tag::pc-managed-state-update-persist-example[]
			Product book = new Product();
			book.setId( 1L );
			book.setName( "High-Performance Java Persistence" );
			book.setDescription( "Get the most out of your persistence layer" );
			book.setPriceCents( 29_99 );
			book.setQuantity( 10_000 );

			entityManager.persist( book );
			//end::pc-managed-state-update-persist-example[]
		} );


		//tag::pc-managed-state-update-example[]
		scope.inTransaction( entityManager -> {
			Product book = entityManager.find( Product.class, 1L );
			book.setPriceCents( 24_99 );
		} );
		//end::pc-managed-state-update-example[]
	}

	//tag::pc-managed-state-update-mapping-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Long id;

		@Column
		private String name;

		@Column
		private String description;

		@Column(name = "price_cents")
		private Integer priceCents;

		@Column
		private Integer quantity;

		//Getters and setters are omitted for brevity

		//end::pc-managed-state-update-mapping-example[]

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

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Integer getPriceCents() {
			return priceCents;
		}

		public void setPriceCents(Integer priceCents) {
			this.priceCents = priceCents;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public void setQuantity(Integer quantity) {
			this.quantity = quantity;
		}
		//tag::pc-managed-state-update-mapping-example[]
	}
	//end::pc-managed-state-update-mapping-example[]
}
