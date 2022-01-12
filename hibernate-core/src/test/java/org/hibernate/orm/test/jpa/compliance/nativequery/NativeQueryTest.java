/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.compliance.nativequery;

import java.util.List;
import java.util.Objects;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Jpa(
		annotatedClasses = { NativeQueryTest.Item.class, NativeQueryTest.Order.class }
)
public class NativeQueryTest {

	@Test
	public void testIt(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					final Item item1 = new Item( 1, "WaterShoes" );
					final Item item2 = new Item( 2, "FlipFlops" );
					final Item item3 = new Item( 3, "Sandals" );

					entityManager.persist( item1 );
					entityManager.persist( item2 );
					entityManager.persist( item3 );

					Order order1 = new Order( 1, 25.0D, item1 );
					entityManager.persist( order1 );
					Order order2 = new Order( 2, 125.0D, item2 );
					entityManager.persist( order2 );
					Order order3 = new Order( 3, 150.0D, item3 );
					entityManager.persist( order3 );

					List<Object[]> results = entityManager.createNativeQuery(
							"Select o.ID, o.ORDER_PRICE, o.ORDER_ITEM , i.ID, i.ITEM_NAME from ORDER_TABLE o, ITEM_TABLE i "
									+ " WHERE (o.ORDER_PRICE > 140) AND (o.ORDER_ITEM = i.ID)",
							"OrderItemResult"
					).getResultList();

					assertEquals( 1, results.size(), "Wrong result size" );

					for ( Object[] result : results ) {
						assertEquals( 2, result.length,  "Wrong number of objects in the array result" );
						for ( Object o : result ) {
							if ( o instanceof Order ) {
								assertEquals( order3, o );
							}
							else if ( o instanceof Item ) {
								assertEquals( item3, o );
							}
							else {
								fail( "Received an unexpected object result:" + o );
							}
						}
					}
				}
		);
	}

	@Entity(name = "Item")
	@Table(name = "ITEM_TABLE")
	public static class Item {

		@Id
		@Column(name = "ID")
		private Integer id;

		@Column(name = "ITEM_NAME")
		private String itemName;

		@OneToOne(mappedBy = "item")
		private Order order;

		public Item() {
		}

		public Item(Integer id, String itemName) {
			this.id = id;
			this.itemName = itemName;
		}

		public Integer getId() {
			return id;
		}

		public Order getOrder() {
			return this.order;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Item item = (Item) o;
			return id == item.id && Objects.equals( itemName, item.itemName ) && Objects.equals(
					order,
					item.order
			);
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, itemName, order );
		}
	}


	@SqlResultSetMapping(name = "OrderItemResult", entities = {
			@EntityResult(entityClass = Order.class),
			@EntityResult(entityClass = Item.class)
	})
	@Entity(name = "Order")
	@Table(name = "ORDER_TABLE")
	public static class Order implements java.io.Serializable {

		@Id
		@Column(name = "ID")
		private Integer id;

		@Column(name = "ORDER_PRICE")
		private double price;

		@OneToOne
		@JoinColumn(name = "ORDER_ITEM")
		private Item item;

		public Order() {
		}

		public Order(Integer id, double price, Item item) {
			this.id = id;
			this.price = price;
			item.order = this;
			this.item = item;
		}

		public Integer getId() {
			return id;
		}

		public double getPrice() {
			return this.price;
		}

		public Item getItem() {
			return item;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Order order = (Order) o;
			return id == order.id && Double.compare( order.price, price ) == 0 && Objects.equals(
					item,
					order.item
			);
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, price, item );
		}
	}

}
