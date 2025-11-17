/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect.join;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/subselect/join/Order.hbm.xml")
@SessionFactory
public class SubselectInJoinedTableTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey("HHH-10998")
	public void testSubselectInJoinedTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			OrderEntry orderEntry1 = new OrderEntry();
			orderEntry1.setOrderEntryId( 1L );
			OrderEntry orderEntry2 = new OrderEntry();
			orderEntry2.setOrderEntryId( 2L );
			Order order = new Order();
			order.setOrderId( 3L );
			order.getOrderEntries().add( orderEntry1 );
			order.getOrderEntries().add( orderEntry2 );
			order.setFirstOrderEntry( orderEntry1 );

			s.persist( orderEntry1 );
			s.persist( orderEntry2 );
			s.persist( order );
		} );

		factoryScope.inTransaction( (s) -> {
			var order = s.find( Order.class, 3L );
			var orderEntry1 = s.find( OrderEntry.class, 1L );
			var orderEntry2 = s.find( OrderEntry.class, 2L );
			assertEquals( orderEntry1.getOrderEntryId(), order.getFirstOrderEntry().getOrderEntryId() );
			assertEquals( 2, order.getOrderEntries().size() );
			assertEquals( orderEntry1.getOrderEntryId(),
					order.getOrderEntries().get( 0 ).getOrderEntryId() );
			assertEquals( orderEntry2.getOrderEntryId(),
					order.getOrderEntries().get( 1 ).getOrderEntryId() );
		} );
	}

	public static class Order {
		private Long orderId;
		private OrderEntry firstOrderEntry;
		private List<OrderEntry> orderEntries = new ArrayList<OrderEntry>();

		public OrderEntry getFirstOrderEntry() {
			return firstOrderEntry;
		}

		public void setFirstOrderEntry(OrderEntry firstOrderEntry) {
			this.firstOrderEntry = firstOrderEntry;
		}

		public Long getOrderId() {
			return orderId;
		}

		public void setOrderId(Long orderId) {
			this.orderId = orderId;
		}

		public List<OrderEntry> getOrderEntries() {
			return orderEntries;
		}

		public void setOrderEntries(List<OrderEntry> orderEntries) {
			this.orderEntries = orderEntries;
		}
	}

	public static class OrderEntry {
		private Long orderEntryId;

		public Long getOrderEntryId() {
			return orderEntryId;
		}

		public void setOrderEntryId(Long orderEntryId) {
			this.orderEntryId = orderEntryId;
		}
	}
}
