/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@DomainModel(
		annotatedClasses = {
				NonPkCompositeJoinColumnCollectionTest.Order.class,
				NonPkCompositeJoinColumnCollectionTest.Item.class,
		}
)
@SessionFactory
public class NonPkCompositeJoinColumnCollectionTest {

	@AfterEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Item" ).executeUpdate();
					session.createMutationQuery( "delete from Order" ).executeUpdate();
				}
		);
	}

	@Test
	public void testCollectionInsertWithNullCollecitonRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Order( null ) );
				}
		);
	}

	@Test
	public void testCollectionInsertEmptyCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Order( "O1" ) );
				}
		);
	}

	@Test
	public void testCollectionInsert(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Order order = new Order( "O1" );
					Item item = new Item( "Item 1" );
					order.addItem( item );
					session.persist( order );
					session.persist( item );
				}
		);
	}

	@Entity(name = "Order")
	@Table(name = "ORDER_TABLE")
	public static class Order {
		@Id
		@GeneratedValue
		public Integer id;

		@Column(name = "uk1")
		String uk1;
		@Column(name = "uk2")
		String uk2;

		@OneToMany
		@JoinColumn(name = "fk1", referencedColumnName = "uk1", insertable = false, updatable = false)
		@JoinColumn(name = "fk2", referencedColumnName = "uk2", insertable = false, updatable = false)
		Collection<Item> items = new ArrayList<>();

		public Order() {
		}

		public Order(String uk) {
			this.uk1 = uk;
			this.uk2 = uk;
		}

		public Integer getId() {
			return id;
		}

		public String getUk1() {
			return uk1;
		}

		public String getUk2() {
			return uk2;
		}

		public Collection<Item> getItems() {
			return items;
		}

		public void addItem(Item item) {
			items.add( item );
			item.fk1 = uk1;
			item.fk2 = uk2;
		}
	}

	@Entity(name = "Item")
	@Table(name = "ITEM_TABLE")
	public static class Item {
		@Id
		@GeneratedValue
		public Integer id;

		public String description;

		@Column(name = "fk1")
		String fk1;
		@Column(name = "fk2")
		String fk2;

		public Item() {
		}

		public Item(String description) {
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
