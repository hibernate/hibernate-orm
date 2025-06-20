/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Chris Cranford
 */
@DomainModel(
		annotatedClasses = {
				FilterDotNameTest.PurchaseOrder.class,
				FilterDotNameTest.PurchaseItem.class
		}
)
@JiraKey(value = "HHH-11250")
public class FilterDotNameTest extends AbstractStatefulStatelessFilterTest {

	@BeforeEach
	void setUp() {
		scope.inTransaction( session -> {
			final PurchaseOrder order1 = new PurchaseOrder( 1L, 10L, 1000L );
			final Set<PurchaseItem> items1 = new HashSet<>();
			items1.add( new PurchaseItem( 1L, 100L, order1 ) );
			items1.add( new PurchaseItem( 2L, 200L, order1 ) );
			order1.setPurchaseItems( items1 );
			session.persist( order1 );

			final PurchaseOrder order2 = new PurchaseOrder( 2L, 20L, 2000L );
			final Set<PurchaseItem> items2 = new HashSet<>();
			items2.add( new PurchaseItem( 3L, 300L, order2 ) );
			items2.add( new PurchaseItem( 4L, 400L, order2 ) );
			order2.setPurchaseItems( items2 );
			session.persist( order2 );
		} );
	}

	@AfterEach
	void tearDown() {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testEntityFilterNameWithoutDots(
			BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "customerIdFilter" ).setParameter( "customerId", 10L );

			final List<PurchaseOrder> orders = session.createQuery( "FROM PurchaseOrder", PurchaseOrder.class ).getResultList();
			assertThat( orders.size(), is( 1 ) );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testEntityFilterNameWithDots(
			BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "PurchaseOrder.customerIdFilter" ).setParameter( "customerId", 20L );

			final List<PurchaseOrder> orders = session.createQuery( "FROM PurchaseOrder", PurchaseOrder.class ).getResultList();
			assertThat( orders.size(), is( 1 ) );
		} );
	}

	@Test
	void testCollectionFilterNameWithoutDots() {
		scope.inTransaction( session -> {
			session.enableFilter( "itemIdFilter" ).setParameter( "itemId", 100L );

			final PurchaseOrder order = session.get( PurchaseOrder.class, 1L );
			assertThat( order.getPurchaseItems().size(), is( 1 ) );
		} );
	}

	@Test
	public void testCollectionFilterNameWithDots() {
		scope.inTransaction( session -> {
			session.enableFilter( "PurchaseOrder.itemIdFilter" ).setParameter( "itemId", 100L );

			final PurchaseOrder order = session.get( PurchaseOrder.class, 1L );
			assertThat( order.getPurchaseItems().size(), is( 1 ) );
		} );
	}

	@Entity(name = "PurchaseOrder")
	@FilterDefs({
			@FilterDef(name = "customerIdFilter", parameters = @ParamDef(name = "customerId", type = Long.class)),
			@FilterDef(name = "PurchaseOrder.customerIdFilter", parameters = @ParamDef(name = "customerId", type = Long.class)),
			@FilterDef(name = "itemIdFilter", parameters = @ParamDef(name = "itemId", type = Long.class)),
			@FilterDef(name = "PurchaseOrder.itemIdFilter", parameters = @ParamDef(name = "itemId", type = Long.class))
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
