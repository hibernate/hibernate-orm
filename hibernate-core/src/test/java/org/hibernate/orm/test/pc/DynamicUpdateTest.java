/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class DynamicUpdateTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {

			Product book = new Product();
			book.setId(1L);
			book.setName("High-Performance Java Persistence");
			book.setDescription("get the most out of your persistence layer");
			book.setPriceCents(29_99);
			book.setQuantity(10_000);

			entityManager.persist(book);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {

			Product book = entityManager.find(Product.class, 1L);
			book.setPriceCents(24_99);
		});
	}

	//tag::pc-managed-state-dynamic-update-mapping-example[]
	@Entity(name = "Product")
	@DynamicUpdate
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

	//end::pc-managed-state-dynamic-update-mapping-example[]

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
	//tag::pc-managed-state-dynamic-update-mapping-example[]
	}
	//end::pc-managed-state-dynamic-update-mapping-example[]
}
