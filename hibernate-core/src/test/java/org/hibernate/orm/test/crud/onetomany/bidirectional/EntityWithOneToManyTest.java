/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud.onetomany.bidirectional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Andrea Boriero
 */
public class EntityWithOneToManyTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class,
				User.class
		};
	}

	@AfterEach
	public void tearDown() {
		inTransaction(
				session -> {
					final User loaded = session.get(
							User.class,
							1
					);

					List<Item> bought = loaded.getBoughtItems();
					bought.forEach( item -> session.remove( item ) );
					session.remove( loaded );
				}
		);
	}

	@Test
	public void testSave() {
		User user = new User( 1, "Fab" );
		Item firstItem = new Item( 2, 100.0 );
		Item secondItem = new Item( 3, 120.0 );
		user.addBoughtItem( firstItem );
		user.addBoughtItem( secondItem );

		inTransaction(
				session -> {
					session.save( user );
					session.save( firstItem );
					session.save( secondItem );
				}
		);

		inTransaction(
				session -> {
					User retrieved = session.get( User.class, 1 );
					assertThat( retrieved, notNullValue() );

					List<Item> boughtItems = retrieved.getBoughtItems();

					assertFalse(
							Hibernate.isInitialized( boughtItems ),
							"The association should ne not initialized"

					);
					assertThat( boughtItems.size(), is( 2 ) );
					Map<Integer, Item> othersById = new HashMap<>();
					for ( Item item : boughtItems ) {
						othersById.put( item.getId(), item );
					}

					assertThat( othersById.get( 2 ).getPrice(), is( 100.0 ) );
					assertThat( othersById.get( 3 ).getPrice(), is( 120.0 ) );
				}
		);
	}

	@Test
	public void testSaveWithoutChildren() {
		User user = new User( 1, "Fab" );
		inTransaction(
				session -> {
					session.save( user );
				}
		);

		inTransaction(
				session -> {
					User retrieved = session.get( User.class, 1 );
					assertThat( retrieved, notNullValue() );

					List<Item> boughtItems = retrieved.getBoughtItems();

					assertThat( boughtItems.size(), is( 0 ) );
				} );
	}

	@Entity(name = "User")
	@Table(name = "USER")
	public static class User {
		private Integer id;
		private String name;
		private List<Item> boughtItems = new ArrayList<>();

		User() {
		}

		public User(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@OneToMany(mappedBy = "buyer")
		public List<Item> getBoughtItems() {
			return boughtItems;
		}

		public void setBoughtItems(List<Item> boughtItems) {
			this.boughtItems = boughtItems;
		}

		public void addBoughtItem(Item item) {
			item.setBuyer( this );
			boughtItems.add( item );
		}
	}

	@Entity(name = "Item")
	@Table(name = "ITEM")
	public static class Item {
		private Integer id;
		private Double price;
		private User buyer;

		Item() {
		}

		public Item(Integer id, Double price) {
			this.id = id;
			this.price = price;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Double getPrice() {
			return price;
		}

		public void setPrice(Double price) {
			this.price = price;
		}

		@ManyToOne
		@JoinTable(
				name = "ITEM_BUYER",
				joinColumns = @JoinColumn(name = "ITEM_ID"),
				inverseJoinColumns = @JoinColumn(name = "BUYER_ID")
		)
		public User getBuyer() {
			return buyer;
		}

		public void setBuyer(User buyer) {
			this.buyer = buyer;
		}
	}
}
