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

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				NonPkJoinColumnCollectionTest.Order.class,
				NonPkJoinColumnCollectionTest.Item.class,
		}
)
@SessionFactory
public class NonPkJoinColumnCollectionTest {

	@AfterEach
	public void setUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testInsertEmptyCollectionWithNullCollecitonRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Persisting an entity with an empty collection and null owning key
					Order order = new Order( null );
					session.persist( order );
					session.flush();
					session.clear();

					// Ensure merging a detached object works
					order.text = "Abc";
					session.merge( order );
					session.flush();
					session.clear();

					Order order1 = session.find( Order.class, order.id );
					assertThat( order1.text ).isEqualTo( "Abc" );
					assertThat( order1.items ).isNull();
				}
		);
	}

	@Test
	public void testInsertCollectionWithNullCollecitonRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Persisting an entity with a non-empty collection though the owning key is null
					// It's somewhat debatable whether this should work by simply ignoring the collection
					// or throw an error that indicates the owning key is missing
					Order order = new Order( null );
					Item item = new Item( "Abc" );
					order.addItem( item );
					session.persist( item );
					session.persist( order );
					session.flush();
					session.clear();

					// Ensure merging a detached object works
					order.text = "Abc";
					session.merge( order );
					session.flush();
					session.clear();

					// Also ensure merging a detached object with a new collection works
					order.items = new ArrayList<>();
					order.addItem( item );
					session.merge( order );
					session.flush();
					session.clear();

					Order order1 = session.find( Order.class, order.id );
					assertThat( order1.text ).isEqualTo( "Abc" );
					assertThat( order1.items ).isNull();
				}
		);
	}

	@Test
	public void testInsertCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Persisting an entity with a collection and non-null owning key
					Order order = new Order( "some_ref" );
					Item item = new Item( "Abc" );
					order.addItem( item );
					session.persist( order );
					session.persist( item );
					session.flush();
					session.clear();

					// Ensure merging a detached object works
					order.text = "Abc";
					session.merge( order );
					session.flush();
					session.clear();

					Order order1 = session.find( Order.class, order.id );
					assertThat( order1.text ).isEqualTo( "Abc" );
					assertThat( order1.items.size() ).isEqualTo( 1 );
					assertThat( order1.items.iterator().next().id ).isEqualTo( item.id );
				}
		);
	}

	@Entity(name = "Order")
	@Table(name = "ORDER_TABLE")
	public static class Order {
		@Id
		@GeneratedValue
		public Integer id;

		String text;

		@Column(name = "c_ref")
		String cRef;

		@OneToMany
		@JoinColumn(name = "p_ref", referencedColumnName = "c_ref", insertable = false, updatable = false)
		Collection<Item> items = new ArrayList<>();

		public Order() {
		}

		public Order(String cRef) {
			this.cRef = cRef;
		}

		public Integer getId() {
			return id;
		}

		public String getcRef() {
			return cRef;
		}

		public Collection<Item> getItems() {
			return items;
		}

		public void addItem(Item item) {
			items.add( item );
			item.pRef = cRef;
		}
	}

	@Entity(name = "Item")
	@Table(name = "ITEM_TABLE")
	public static class Item {
		@Id
		@GeneratedValue
		public Integer id;

		public String description;

		@Column(name = "p_ref")
		String pRef;

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
