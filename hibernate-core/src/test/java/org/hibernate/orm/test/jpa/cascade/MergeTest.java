/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {
		MergeTest.Order.class,
		MergeTest.Item.class
})
@SessionFactory
public class MergeTest {

	@Test
	public void testMergeDetachedEntityWithNewOneToManyElements(SessionFactoryScope scope) {
		Order order = new Order();

		scope.inTransaction(
				session -> {
					session.persist( order );
				}
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
					Order _order = session.find( Order.class, order.id );
					assertEquals( 2, _order.items.size() );
					session.remove( _order );
				}
		);
	}

	@Test
	public void testMergeLoadedEntityWithNewOneToManyElements(SessionFactoryScope scope) {
		Order order = new Order();

		scope.inTransaction(
				session -> {
					session.persist( order );
				}
		);

		scope.inTransaction(
				session -> {
					Order _order = session.find( Order.class, order.id );
					Item item1 = new Item();
					item1.name = "i1";
					Item item2 = new Item();
					item2.name = "i2";
					_order.addItem( item1 );
					_order.addItem( item2 );
					session.merge( _order );
					session.flush();
				}
		);

		scope.inTransaction(
				session -> {
					Order _order = session.find( Order.class, order.id );
					assertEquals( 2, _order.items.size() );
					session.remove( _order );
				}
		);
	}

	@Entity
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

	@Entity
	public static class Item {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		private Order order;
	}
}
