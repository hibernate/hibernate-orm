/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {
		MergeTest.Order.class,
		MergeTest.Item.class
})
public class MergeTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMergeDetachedEntityWithNewOneToManyElements(EntityManagerFactoryScope scope) {
		Order order = new Order();

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( order );
				}
		);

		Item item1 = new Item();
		item1.name = "i1";

		Item item2 = new Item();
		item2.name = "i2";

		order.addItem( item1 );
		order.addItem( item2 );

		scope.inTransaction(
				entityManager -> {
					entityManager.merge( order );
					entityManager.flush();
				}
		);

		scope.inTransaction(
				entityManager -> {
					Order _order = entityManager.find( Order.class, order.id );
					assertEquals( 2, _order.items.size() );
				}
		);
	}

	@Test
	public void testMergeLoadedEntityWithNewOneToManyElements(EntityManagerFactoryScope scope) {
		Order order = new Order();

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( order );
				}
		);

		scope.inTransaction(
				entityManager -> {
					Order _order = entityManager.find( Order.class, order.id );
					Item item1 = new Item();
					item1.name = "i1";
					Item item2 = new Item();
					item2.name = "i2";
					_order.addItem( item1 );
					_order.addItem( item2 );
					entityManager.merge( _order );
					entityManager.flush();
				}
		);

		scope.inTransaction(
				entityManager -> {
					Order _order = entityManager.find( Order.class, order.id );
					assertEquals( 2, _order.items.size() );
				}
		);
	}

	@Entity(name = "Order")
	@Table(name = "orders")
	public static class Order {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "order", orphanRemoval = true)
		private List<Item> items = new ArrayList<Item>();

		public Order() {
		}

		public void addItem(Item item) {
			items.add( item );
			item.order = this;
		}
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		private Order order;
	}
}
