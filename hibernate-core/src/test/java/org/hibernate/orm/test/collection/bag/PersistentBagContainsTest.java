/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.bag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.processing.Exclude;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to contains operations on a PersistentBag.
 *
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				PersistentBagContainsTest.Order.class,
				PersistentBagContainsTest.Item.class
		}
)
@SessionFactory
@Exclude
public class PersistentBagContainsTest {

	/**
	 * This test does not verify how equals is implemented for Bags,
	 * but rather if the child entity equals and hashCode are used properly for both
	 * managed and detached entities.
	 */
	@Test
	@JiraKey(value = "HHH-5409")
	public void testContains(SessionFactoryScope scope) {
		Order _order = scope.fromTransaction( session -> {
			Order order = new Order();
			session.persist( order );

			Item item1 = new Item();
			item1.setName( "i1" );
			Item item2 = new Item();
			item2.setName( "i2" );
			order.addItem( item1 );
			order.addItem( item2 );

			return order;
		} );

		scope.inTransaction( session -> {
			Item item1 = new Item();
			item1.setName( "i1" );

			Item item2 = new Item();
			item2.setName( "i2" );

			assertTrue( _order.getItems().contains( item1 ) );
			assertTrue( _order.getItems().contains( item2 ) );

			Order order = session.find( Order.class, _order.getId() );

			assertTrue( order.getItems().contains( item1 ) );
			assertTrue( order.getItems().contains( item2 ) );
		} );

		scope.inTransaction( session -> {
			Order order = session.find( Order.class, _order.getId() );
			session.remove( order );
		} );
	}

	@Entity(name = "`Order`")
	public static class Order {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Item> items = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Item> getItems() {
			return items;
		}

		public void addItem(Item item) {
			items.add( item );
			item.setOrder( this );
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

		public Order getOrder() {
			return order;
		}

		public void setOrder(Order order) {
			this.order = order;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Item ) ) {
				return false;
			}
			Item item = (Item) o;
			return Objects.equals( getName(), item.getName() );
		}

		@Override
		public int hashCode() {
			return Objects.hash( getName() );
		}
	}

}
