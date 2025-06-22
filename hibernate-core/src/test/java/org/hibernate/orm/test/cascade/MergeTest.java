/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DomainModel(
		annotatedClasses = {
				MergeTest.Order.class,
				MergeTest.Item.class
		}
)
@SessionFactory
public class MergeTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMergeDetachedEntityWithNewOneToManyElements(SessionFactoryScope scope) {
		Order order = new Order();

		scope.inTransaction(
				session ->
						session.persist( order )
		);

		Item item1 = new Item();
		item1.name = "i1";

		Item item2 = new Item();
		item2.name = "i2";

		order.addItem( item1 );
		order.addItem( item2 );

		scope.inTransaction(
				session -> {
					session.merge( order );
					session.flush();
				}
		);

		scope.inTransaction(
				session -> {
					Order result = session.get( Order.class, order.id );
					assertEquals( 2, result.items.size() );
				}
		);
	}

	@Test
	public void testMergeEntityWithNewOneToManyElements(SessionFactoryScope scope) {
		Order order = new Order();

		scope.inTransaction(
				session ->
						session.persist( order )
		);

		scope.inTransaction(
				session -> {
					Order result = session.get( Order.class, order.id );
					Item item1 = new Item();
					item1.name = "i1";
					Item item2 = new Item();
					item2.name = "i2";
					result.addItem( item1 );
					result.addItem( item2 );
					assertFalse( Hibernate.isInitialized( result.items ) );
					session.merge( result );
				}
		);

		scope.inTransaction(
				session -> {
					Order result = session.get( Order.class, order.id );
					assertEquals( 2, result.items.size() );
				}
		);
	}

	@Entity(name = "Order")
	@Table( name = "`ORDER`" )
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
