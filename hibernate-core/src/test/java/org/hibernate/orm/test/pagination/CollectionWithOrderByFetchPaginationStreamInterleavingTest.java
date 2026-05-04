/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import org.hibernate.ScrollMode;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code scroll()} and {@code getResultStream()} go through
 * {@code FetchingScrollableResultsImpl}, which groups physical rows into one
 * logical result only while the root entity key stays consecutive. This test
 * forces the fetched collection rows to interleave across roots:
 * <ol>
 * <li>Only two parents exist, so {@code setMaxResults(2)} always selects both
 *     parents even without an explicit root sort.</li>
 * <li>The child collection is {@link OrderBy @OrderBy("id")}-ordered.</li>
 * <li>Child ids are inserted alternating across parents: cart1={1,3,5},
 *     cart2={2,4,6}.</li>
 * </ol>
 * Without serialising the outer fetch joins by owner, the SQL row order can be
 * {@code cart1/item1, cart2/item2, cart1/item3, ...}, and the stream would
 * emit duplicate parents with partial collections.
 */
@DomainModel(annotatedClasses = {
		CollectionWithOrderByFetchPaginationStreamInterleavingTest.Cart.class,
		CollectionWithOrderByFetchPaginationStreamInterleavingTest.LineItem.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "true"
))
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
public class CollectionWithOrderByFetchPaginationStreamInterleavingTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final Cart cart1 = new Cart( 1L, "Cart 1" );
			final Cart cart2 = new Cart( 2L, "Cart 2" );
			s.persist( cart1 );
			s.persist( cart2 );

			persistLineItem( s, cart1, 1L, "Item 1" );
			persistLineItem( s, cart2, 2L, "Item 2" );
			persistLineItem( s, cart1, 3L, "Item 3" );
			persistLineItem( s, cart2, 4L, "Item 4" );
			persistLineItem( s, cart1, 5L, "Item 5" );
			persistLineItem( s, cart2, 6L, "Item 6" );
		} );
	}

	private static void persistLineItem(org.hibernate.Session session, Cart cart, long id, String name) {
		final LineItem item = new LineItem( id, name );
		cart.addLineItem( item );
		session.persist( item );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void streamWithInterleavedOrderedCollectionRows(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Cart> carts;
			try ( var stream = s.createSelectionQuery(
					"from Cart c left join fetch c.lineItems",
					Cart.class
			).setMaxResults( 2 ).getResultStream() ) {
				carts = stream.collect(toList());
			}

			assertCartResults( carts );
			assertOwnerGroupingOrder( sql );
		} );
	}

	@Test
	void scrollWithInterleavedOrderedCollectionRows(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Cart> carts = new ArrayList<>();
			try ( var scroll = s.createSelectionQuery(
					"from Cart c left join fetch c.lineItems",
					Cart.class
			).setMaxResults( 2 ).scroll( ScrollMode.FORWARD_ONLY ) ) {
				while ( scroll.next() ) {
					carts.add( scroll.get() );
				}
			}

			assertCartResults( carts );
			assertOwnerGroupingOrder( sql );
		} );
	}

	private static void assertCartResults(List<Cart> carts) {
		assertEquals( 2, carts.size() );
		carts.sort( Comparator.comparing( Cart::getId ) );
		assertEquals( 1L, carts.get( 0 ).getId() );
		assertEquals( 2L, carts.get( 1 ).getId() );
		assertEquals( List.of( 1L, 3L, 5L ), carts.get( 0 ).getLineItems().stream().map( LineItem::getId ).toList() );
		assertEquals( List.of( 2L, 4L, 6L ), carts.get( 1 ).getLineItems().stream().map( LineItem::getId ).toList() );
	}

	private static void assertOwnerGroupingOrder(SQLStatementInspector sql) {
		assertEquals( 1, sql.getSqlQueries().size() );
		final String normalizedSql = sql.getSqlQueries().get( 0 )
				.toLowerCase( Locale.ROOT )
				.replaceAll( "\\s+", " " );
		assertTrue( normalizedSql.contains( "from (select" ) );
		final int orderBy = normalizedSql.indexOf( " order by " );
		final int ownerIdOrder = normalizedSql.indexOf( "c1_0.id", orderBy );
		final int itemIdOrder = normalizedSql.indexOf( "li1_0.id", ownerIdOrder + 1 );
		assertTrue( orderBy > 0 );
		assertTrue( ownerIdOrder > orderBy );
		assertTrue( itemIdOrder > ownerIdOrder );
	}

	@Entity(name = "Cart")
	public static class Cart {
		@Id
		private Long id;
		private String name;

		@Override
		public String toString() {
			return name;
		}

		@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL)
		@OrderBy("id")
		private List<LineItem> lineItems = new ArrayList<>();

		public Cart() {
		}

		public Cart(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<LineItem> getLineItems() {
			return lineItems;
		}

		public void addLineItem(LineItem item) {
			lineItems.add( item );
			item.setCart( this );
		}
	}

	@Entity(name = "LineItem")
	public static class LineItem {
		@Id
		private Long id;
		private String name;

		@ManyToOne
		private Cart cart;

		public LineItem() {
		}

		public LineItem(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Cart getCart() {
			return cart;
		}

		public void setCart(Cart cart) {
			this.cart = cart;
		}
	}
}
