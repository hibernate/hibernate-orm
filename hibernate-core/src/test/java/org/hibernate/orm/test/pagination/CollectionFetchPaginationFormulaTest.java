/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Formula;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ahmed El amraouiyine
 */
@DomainModel(annotatedClasses = {
		CollectionFetchPaginationFormulaTest.PurchaseOrder.class,
		CollectionFetchPaginationFormulaTest.PurchaseOrderItem.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "true"
))
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
public class CollectionFetchPaginationFormulaTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long i = 1; i <= 3; i++ ) {
				final PurchaseOrder order = new PurchaseOrder( i );
				order.addItem( new PurchaseOrderItem( i * 10 + 1 ) );
				order.addItem( new PurchaseOrderItem( i * 10 + 2 ) );
				session.persist( order );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void formulaSelectableUsesSafeAliasInPaginatedCollectionFetch(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			sql.clear();

			final List<PurchaseOrder> orders = session.createSelectionQuery(
					"from PurchaseOrder o left join fetch o.items order by o.id",
					PurchaseOrder.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, orders.size() );
			assertEquals( 2, orders.get( 0 ).getItemCount() );
			assertEquals( 2, orders.get( 0 ).getItems().size() );
			assertEquals( 2, orders.get( 1 ).getItemCount() );
			assertEquals( 2, orders.get( 1 ).getItems().size() );

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
			assertFalse( generated.contains( "{@}" ) );
		} );
	}

	@Entity(name = "PurchaseOrder")
	@Table(name = "purchase_order")
	public static class PurchaseOrder {
		@Id
		private Long id;

		@Formula("(select count(i.id) from purchase_order_item i where i.order_id = id)")
		private int itemCount;

		@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
		private List<PurchaseOrderItem> items = new ArrayList<>();

		public PurchaseOrder() {
		}

		public PurchaseOrder(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public int getItemCount() {
			return itemCount;
		}

		public List<PurchaseOrderItem> getItems() {
			return items;
		}

		public void addItem(PurchaseOrderItem item) {
			items.add( item );
			item.setOrder( this );
		}
	}

	@Entity(name = "PurchaseOrderItem")
	@Table(name = "purchase_order_item")
	public static class PurchaseOrderItem {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "order_id")
		private PurchaseOrder order;

		public PurchaseOrderItem() {
		}

		public PurchaseOrderItem(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public PurchaseOrder getOrder() {
			return order;
		}

		public void setOrder(PurchaseOrder order) {
			this.order = order;
		}
	}
}
