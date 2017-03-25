/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cascade;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.assertEquals;

public class MergeTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testMergeDetachedEntityWithNewOneToManyElements() {
		Order order = new Order();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( order );
		em.getTransaction().commit();
		em.close();

		Item item1 = new Item();
		item1.name = "i1";

		Item item2 = new Item();
		item2.name = "i2";

		order.addItem( item1 );
		order.addItem( item2 );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		order = em.merge( order );
		em.flush();
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		order = em.find( Order.class, order.id );
		assertEquals( 2, order.items.size() );
		em.remove( order );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMergeLoadedEntityWithNewOneToManyElements() {
		Order order = new Order();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( order );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		order = em.find( Order.class, order.id );
		Item item1 = new Item();
		item1.name = "i1";
		Item item2 = new Item();
		item2.name = "i2";
		order.addItem( item1 );
		order.addItem( item2 );
		order = em.merge( order );
		em.flush();
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		order = em.find( Order.class, order.id );
		assertEquals( 2, order.items.size() );
		em.remove( order );
		em.getTransaction().commit();
		em.close();
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
