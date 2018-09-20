/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.bag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to contains operations on a PersistentBag.
 *
 * @author Vlad Mihalcea
 */
public class PersistentBagContainsTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Order.class,
			Item.class
		};
	}

	/**
	 * This test does not verify how equals is implemented for Bags,
	 * but rather if the child entity equals and hashCode are used properly for both
	 * managed and detached entities.
	 */
	@Test
	@TestForIssue( jiraKey = "HHH-5409")
	public void testContains() {
		Order _order = doInHibernate( this::sessionFactory, session -> {
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

		doInHibernate( this::sessionFactory, session -> {
			Item item1 = new Item();
			item1.setName( "i1" );

			Item item2 = new Item();
			item2.setName( "i2" );

			assertTrue(_order.getItems().contains( item1 ));
			assertTrue(_order.getItems().contains( item2 ));

			Order order = session.find( Order.class, _order.getId() );

			assertTrue(order.getItems().contains( item1 ));
			assertTrue(order.getItems().contains( item2 ));
		} );

		doInHibernate( this::sessionFactory, session -> {
			Order order = session.find( Order.class, _order.getId() );
			session.delete( order );
		} );
	}

	@Entity(name = "`Order`")
	public static class Order {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Item> items = new ArrayList<Item>();

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
