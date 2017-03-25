/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.subselect.join;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

public class SubselectInJoinedTableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] {
				"subselect/join/Order.hbm.xml"
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10998")
	public void testSubselectInJoinedTable() {
		OrderEntry orderEntry1 = new OrderEntry();
		orderEntry1.setOrderEntryId( 1L );
		OrderEntry orderEntry2 = new OrderEntry();
		orderEntry2.setOrderEntryId( 2L );
		Order order = new Order();
		order.setOrderId( 3L );
		order.getOrderEntries().add( orderEntry1 );
		order.getOrderEntries().add( orderEntry2 );
		order.setFirstOrderEntry( orderEntry1 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( orderEntry1 );
		s.persist( orderEntry2 );
		s.persist( order );
		tx.commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		order = (Order) s.get( Order.class, order.getOrderId() );
		assertEquals( orderEntry1.getOrderEntryId(), order.getFirstOrderEntry().getOrderEntryId() );
		assertEquals( 2, order.getOrderEntries().size() );
		assertEquals( orderEntry1.getOrderEntryId(), order.getOrderEntries().get( 0 ).getOrderEntryId() );
		assertEquals( orderEntry2.getOrderEntryId(), order.getOrderEntries().get( 1 ).getOrderEntryId() );
		s.getTransaction().commit();
		s.close();

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
