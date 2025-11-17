/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				SetAndBagCollectionTest.Customer.class,
				SetAndBagCollectionTest.Order.class,
				SetAndBagCollectionTest.Item.class,
		}
)
@SessionFactory
public class SetAndBagCollectionTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Customer customer = new Customer( 1, "First" );
					Order order1 = new Order( 1, "First Order" );
					Order order2 = new Order( 2, "Second Order" );

					customer.addOrder( order1 );
					customer.addOrder( order2 );

					Item item1 = new Item( 1, "first" );
					Item item2 = new Item( 2, "second" );
					Item item3 = new Item( 3, "third" );

					order1.addItem( item1 );
					order1.addItem( item2 );
					order1.addItem( item3 );
					order1.addItem( item3 );

					Item item4 = new Item( 4, "fourth" );
					Item item5 = new Item( 5, "fifth" );

					order2.addItem( item4 );
					order2.addItem( item5 );

					session.persist( item1 );
					session.persist( item2 );
					session.persist( item3 );
					session.persist( item4 );
					session.persist( item5 );

					session.persist( order1 );
					session.persist( order2 );

					session.persist( customer );
				}
		);
	}

	@Test
	public void testThatRetrievedBagElementsAreofTheRightCardinality(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Customer customer = session.get( Customer.class, 1 );
					Set<Order> orders = customer.getOrders();

					assertThat( orders.size() ).isEqualTo( 2 );

					orders.forEach(
							order -> {
								Collection<Item> items = order.getItems();
								if ( order.getId() == 1 ) {
									assertThat( items.size() ).isEqualTo( 4 );
								}
								else {
									assertThat( items.size() ).isEqualTo( 2 );
								}
							}
					);

				}
		);
	}

	@Entity(name = "Customer")
	public static class Customer {
		@Id
		public Integer id;

		public String name;

		@OneToMany
		Set<Order> orders = new HashSet<>();

		public Customer() {
		}

		public Customer(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<Order> getOrders() {
			return orders;
		}

		public void addOrder(Order order) {
			orders.add( order );
		}
	}

	@Entity(name = "Order")
	@Table(name = "ORDER_TABLE")
	public static class Order {
		@Id
		public Integer id;

		public String description;

		@ManyToMany(fetch = FetchType.EAGER)
		Collection<Item> items = new ArrayList<>();

		public Order() {
		}

		public Order(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		public Integer getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}

		public Collection<Item> getItems() {
			return items;
		}

		public void addItem(Item item) {
			items.add( item );
			item.orders.add( this);
		}
	}

	@Entity(name = "Item")
	@Table(name = "ITEM_TABLE")
	public static class Item {
		@Id
		public Integer id;

		public String description;

		@ManyToMany(mappedBy = "items")
		public Collection<Order> orders = new ArrayList<>();

		public Item() {
		}

		public Item(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		public Integer getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}
	}
}
