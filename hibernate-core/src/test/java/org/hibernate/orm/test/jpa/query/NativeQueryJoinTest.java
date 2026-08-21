/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				NativeQueryJoinTest.Order.class,
				NativeQueryJoinTest.OrderInfo.class,
		}
)
@SessionFactory
@JiraKey("HHH-19524")
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsNumericPrimaryKey.class)
public class NativeQueryJoinTest {

	private static final long ORDER_ID = 1L;
	private static final long ORDER_INFO_ID = 2L;
	private static final String ORDER_INFO_DESCRIPTION = "first order";

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					OrderInfo additionalInfo = new OrderInfo(
							new BigDecimal( ORDER_INFO_ID ),
							ORDER_INFO_DESCRIPTION
					);
					Order order = new Order( ORDER_ID, 1L, additionalInfo );
					session.persist( order );
				}
		);
	}

	@AfterEach
	public void teardown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Order order = session.find( Order.class, ORDER_ID );
					OrderInfo additionalInfo = order.getOrderInfo();
					assertThat( additionalInfo.getDescription() ).isEqualTo( ORDER_INFO_DESCRIPTION );
				} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Long> ids = session.createQuery("select o.orderId from Order o", Long.class ).list();
					session.createMutationQuery( "update Order set description = :des" ).setParameter( "des", "abc" ).executeUpdate();
				} );
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String query = "select o.* from ORDER_TABLE o where o.ORDER_ID = ?1";
					List<Order> orders = session.createNativeQuery( query, Order.class )
							.setParameter( 1, ORDER_ID )
							.list();
					Order order = orders.get( 0 );
					OrderInfo additionalInfo = order.getOrderInfo();
					assertThat( additionalInfo.getDescription() ).isEqualTo( ORDER_INFO_DESCRIPTION );
				} );
	}

	@Table(name = "ORDER_TABLE")
	@Entity(name = "Order")
	public static class Order {

		@Id
		@Column(name = "ORDER_ID")
		private Long orderId;

		private long orderNumber;

		private String description;

		@OneToOne(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private OrderInfo orderInfo;

		public Order() {
		}

		public Long getOrderId() {
			return orderId;
		}

		public long getOrderNumber() {
			return orderNumber;
		}

		public OrderInfo getOrderInfo() {
			return orderInfo;
		}

		public Order(Long orderId, long orderNumber, OrderInfo additionalInfo) {
			this.orderId = orderId;
			this.orderNumber = orderNumber;
			this.orderInfo = additionalInfo;
			additionalInfo.order = this;
		}
	}

	@Entity(name = "OrderInfo")
	@Table(name = "ORDER_INFO_TABLE")
	public static class OrderInfo {

		@Id
		@Column(name = "ORDER_INFO_ID")
		private BigDecimal orderInfoId;

		private String description;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "ORDER_ID")
		private Order order;

		public OrderInfo() {
		}

		public OrderInfo(BigDecimal orderInfoId, String description) {
			this.orderInfoId = orderInfoId;
			this.description = description;
		}

		public BigDecimal getOrderInfoId() {
			return orderInfoId;
		}

		public String getDescription() {
			return description;
		}

		public Order getOrder() {
			return order;
		}
	}
}
