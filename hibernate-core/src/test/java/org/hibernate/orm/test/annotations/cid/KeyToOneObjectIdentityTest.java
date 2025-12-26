/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.cid;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				KeyToOneObjectIdentityTest.Customer.class,
				KeyToOneObjectIdentityTest.Order.class,
				KeyToOneObjectIdentityTest.LineItem.class,
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-9579")
public class KeyToOneObjectIdentityTest {

	@Test
	public void test(SessionFactoryScope scope) {
		final String customerName = "customerName";
		final String product = "The Special Box";
		final String orderRef = "orderRef1234";
		final int quantity = 42;

		LineItem.Id lineItemId = scope.fromTransaction( s -> {
			Customer customer = new Customer( 1L, customerName );
			s.persist( customer );
			Order order = new Order( 1L, customer, BigDecimal.ONE, orderRef );
			s.persist( order );
			LineItem lineItem = new LineItem( new LineItem.Id( order, 0 ) );
			lineItem.setLineItemIndex( 0 );
			lineItem.setProduct( product );
			lineItem.setQuantity( quantity );
			s.persist( lineItem );
			return lineItem.getId();
		} );

		scope.getSessionFactory().getCache().evictAllRegions();
		scope.inTransaction( s -> {
			LineItem lineItem = s.get( LineItem.class, lineItemId );
			assertNotNull( lineItem );
			assertEquals( product, lineItem.getProduct() );
			assertEquals( quantity, lineItem.getQuantity() );
			assertTrue( s.contains( lineItem ) );
			LineItem.Id id = lineItem.getId();
			assertNotNull( id );

			Order order = id.getOrder();
			assertNotNull( order );
			assertEquals( orderRef, order.getOrderRef() );
			assertTrue( s.contains( order ) );

			Customer customer = order.getCustomer();
			assertNotNull( customer );
			assertEquals( customerName, customer.getName() );
			assertTrue( s.contains( customer ) );
		} );
	}

	@Entity(name = "Customer")
	public static class Customer {
		@Id
		private long customerId;
		private String name;

		public Customer() {
		}

		public Customer(long customerId, String name) {
			this.customerId = customerId;
			this.name = name;
		}

		public long getCustomerId() {
			return customerId;
		}

		public void setCustomerId(long customerId) {
			this.customerId = customerId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Order")
	@Table(name = "ord_tbl")
	public static class Order {
		@Id
		private long orderId;
		@ManyToOne(fetch = FetchType.LAZY)
		private Customer customer;
		private BigDecimal total;
		private String orderRef;

		public Order() {
		}

		public Order(long orderId, Customer customer, BigDecimal total, String orderRef) {
			this.orderId = orderId;
			this.customer = customer;
			this.total = total;
			this.orderRef = orderRef;
		}

		public long getOrderId() {
			return orderId;
		}

		public void setOrderId(long orderId) {
			this.orderId = orderId;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		public BigDecimal getTotal() {
			return total;
		}

		public void setTotal(BigDecimal total) {
			this.total = total;
		}

		public String getOrderRef() {
			return orderRef;
		}

		public void setOrderRef(String orderRef) {
			this.orderRef = orderRef;
		}
	}

	@Entity(name = "LineItem")
	public static class LineItem {

		@Embeddable
		public static class Id {
			@ManyToOne(fetch = FetchType.EAGER)
			private Order order;
			private int orderLineId;

			public Id() {
			}

			public Id(Order order, int orderLineId) {
				setOrder( order );
				setOrderLineId( orderLineId );
			}

			public Order getOrder() {
				return order;
			}

			public void setOrder(Order order) {
				this.order = order;
			}

			public int getOrderLineId() {
				return orderLineId;
			}

			public void setOrderLineId(int orderLineId) {
				this.orderLineId = orderLineId;
			}

			@Override
			public final boolean equals(Object o) {
				if ( !( o instanceof Id id ) ) {
					return false;
				}

				return orderLineId == id.orderLineId && Objects.equals( order, id.order );
			}

			@Override
			public int hashCode() {
				int result = Objects.hashCode( order );
				result = 31 * result + orderLineId;
				return result;
			}
		}

		@EmbeddedId
		private Id id = new Id();
		private int lineItemIndex;
		private int quantity;
		//	private Order order;
		private String product;

		public LineItem(Id id) {
			setId( id );
		}

		public LineItem() {
		}

		public Id getId() {
			return id;
		}

		public void setId(Id id) {
			this.id = id;
		}

		public int getLineItemIndex() {
			return lineItemIndex;
		}

		public void setLineItemIndex(int lineItemIndex) {
			this.lineItemIndex = lineItemIndex;
		}

		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}

		public String getProduct() {
			return product;
		}

		public void setProduct(String product) {
			this.product = product;
		}
	}
}
