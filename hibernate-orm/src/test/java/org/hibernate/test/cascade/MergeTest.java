/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cascade;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MergeTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testMergeDetachedEntityWithNewOneToManyElements() {
		Order order = new Order();

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( order );
		s.getTransaction().commit();
		s.close();

		Item item1 = new Item();
		item1.name = "i1";

		Item item2 = new Item();
		item2.name = "i2";

		order.addItem( item1 );
		order.addItem( item2 );

		s = openSession();
		s.getTransaction().begin();
		order = (Order) s.merge( order );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		order = s.get( Order.class, order.id );
		assertEquals( 2, order.items.size() );
		s.delete( order );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMergeEntityWithNewOneToManyElements() {
		Order order = new Order();

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( order );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		order = s.get( Order.class, order.id );
		Item item1 = new Item();
		item1.name = "i1";
		Item item2 = new Item();
		item2.name = "i2";
		order.addItem( item1 );
		order.addItem( item2 );
		assertFalse( Hibernate.isInitialized( order.items ) );
		order = (Order) s.merge( order );
		//s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		order = s.get( Order.class, order.id );
		assertEquals( 2, order.items.size() );
		s.delete( order );
		s.getTransaction().commit();
		s.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Order.class,
				Item.class
		};
	}

	@Entity
	private static class Order {
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

	@Entity
	private static class Item {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		private Order order;
	}
}
