/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.pc;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class NoDynamicUpdateTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {

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
		doInJPA( this::entityManagerFactory, entityManager -> {
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
