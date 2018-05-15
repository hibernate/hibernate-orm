/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11250")
public class FilterDotNameTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PurchaseOrder.class, PurchaseItem.class };
	}

	@Override
	protected void prepareTest() throws Exception {

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "DELETE FROM PurchaseItem" ).executeUpdate();
			session.createQuery( "DELETE FROM PurchaseOrder" ).executeUpdate();
			session.flush();
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			PurchaseOrder order1 = new PurchaseOrder( 1L, 10L, 1000L );
			Set<PurchaseItem> items1 = new HashSet<>();
			items1.add( new PurchaseItem( 1L, 100L, order1 ) );
			items1.add( new PurchaseItem( 2L, 200L, order1 ) );
			order1.setPurchaseItems( items1 );
			session.persist( order1 );

			PurchaseOrder order2 = new PurchaseOrder( 2L, 20L, 2000L );
			Set<PurchaseItem> items2 = new HashSet<>();
			items2.add( new PurchaseItem( 3L, 300L, order2 ) );
			items2.add( new PurchaseItem( 4L, 400L, order2 ) );
			order2.setPurchaseItems( items2 );
			session.persist( order2 );
		} );
	}

	@Test
	public void testEntityFilterNameWithoutDots() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.enableFilter( "customerIdFilter" ).setParameter( "customerId", 10L );

			List<PurchaseOrder> orders = session.createQuery( "FROM PurchaseOrder", PurchaseOrder.class ).getResultList();
			assertEquals( 1, orders.size() );
		} );
	}

	@Test
	public void testEntityFilterNameWithDots() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.enableFilter( "PurchaseOrder.customerIdFilter" ).setParameter( "customerId", 20L );

			List<PurchaseOrder> orders = session.createQuery( "FROM PurchaseOrder", PurchaseOrder.class ).getResultList();
			assertEquals( 1, orders.size() );
		} );
	}

	@Test
	public void testCollectionFilterNameWithoutDots() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.enableFilter( "itemIdFilter" ).setParameter( "itemId", 100L );

			PurchaseOrder order = session.get( PurchaseOrder.class, 1L );
			assertEquals( 1, order.getPurchaseItems().size() );
		} );
	}

	@Test
	public void testCollectionFilterNameWithDots() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.enableFilter( "PurchaseOrder.itemIdFilter" ).setParameter( "itemId", 100L );

			PurchaseOrder order = session.get( PurchaseOrder.class, 1L );
			assertEquals( 1, order.getPurchaseItems().size() );
		} );
	}

	@Entity(name = "PurchaseOrder")
	@FilterDefs({
			@FilterDef(name = "customerIdFilter", parameters = @ParamDef(name = "customerId", type = "long")),
			@FilterDef(name = "PurchaseOrder.customerIdFilter", parameters = @ParamDef(name = "customerId", type = "long")),
			@FilterDef(name = "itemIdFilter", parameters = @ParamDef(name = "itemId", type = "long")),
			@FilterDef(name = "PurchaseOrder.itemIdFilter", parameters = @ParamDef(name = "itemId", type = "long"))
	})
	@Filters({
			@Filter(name = "customerIdFilter", condition = "customerId = :customerId"),
			@Filter(name = "PurchaseOrder.customerIdFilter", condition = "customerId = :customerId")
	})
	public static class PurchaseOrder {
		@Id
		private Long purchaseOrderId;
		private Long customerId;
		private Long totalAmount;

		@OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.PERSIST)
		@Filters({
				@Filter(name = "itemIdFilter", condition = "itemId = :itemId"),
				@Filter(name = "PurchaseOrder.itemIdFilter", condition = "itemId = :itemId")
		})
		private Set<PurchaseItem> purchaseItems;

		public PurchaseOrder() {
		}

		public PurchaseOrder(Long purchaseOrderId, Long customerId, Long totalAmount) {
			this.purchaseOrderId = purchaseOrderId;
			this.customerId = customerId;
			this.totalAmount = totalAmount;
		}

		public Long getPurchaseOrderId() {
			return purchaseOrderId;
		}

		public void setPurchaseOrderId(Long purchaseOrderId) {
			this.purchaseOrderId = purchaseOrderId;
		}

		public Long getCustomerId() {
			return customerId;
		}

		public void setCustomerId(Long customerId) {
			this.customerId = customerId;
		}

		public Long getTotalAmount() {
			return totalAmount;
		}

		public void setTotalAmount(Long totalAmount) {
			this.totalAmount = totalAmount;
		}

		public Set<PurchaseItem> getPurchaseItems() {
			return purchaseItems;
		}

		public void setPurchaseItems(Set<PurchaseItem> purchaseItems) {
			this.purchaseItems = purchaseItems;
		}
	}

	@Entity(name = "PurchaseItem")
	public static class PurchaseItem {
		@Id
		private Long purchaseItemId;
		private Long itemId;

		@ManyToOne
		@JoinColumn(name = "purchaseOrderId")
		private PurchaseOrder purchaseOrder;

		public PurchaseItem() {
		}

		public PurchaseItem(Long purchaseItemId, Long itemId, PurchaseOrder purchaseOrder) {
			this.purchaseItemId = purchaseItemId;
			this.itemId = itemId;
			this.purchaseOrder = purchaseOrder;
		}

		public Long getPurchaseItemId() {
			return purchaseItemId;
		}

		public void setPurchaseItemId(Long purchaseItemId) {
			this.purchaseItemId = purchaseItemId;
		}

		public Long getItemId() {
			return itemId;
		}

		public void setItemId(Long itemId) {
			this.itemId = itemId;
		}

		public PurchaseOrder getPurchaseOrder() {
			return purchaseOrder;
		}

		public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
			this.purchaseOrder = purchaseOrder;
		}
	}
}
