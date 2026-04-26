/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.ScrollMode;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Same alternating insert pattern as the ordered variant, but without an
 * explicit collection {@code order by}. The important part here is that
 * scroll-style execution still stabilises the outer query by owner id, making
 * the stream deterministic even when the database would otherwise be free to
 * choose the join row order.
 */
@DomainModel(annotatedClasses = {
		CollectionFetchPaginationStreamInterleavingTest.Cart.class,
		CollectionFetchPaginationStreamInterleavingTest.LineItem.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "true"
))
@SessionFactory(useCollectingStatementInspector = true)
@SkipForDialect( dialectClass = SybaseASEDialect.class )
public class CollectionFetchPaginationStreamInterleavingTest {

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
	void streamWithInterleavedCollectionRows(SessionFactoryScope scope) {
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
	void scrollWithInterleavedCollectionRows(SessionFactoryScope scope) {
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
		assertTrue( orderBy > 0 );
		assertTrue( ownerIdOrder > orderBy );
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
