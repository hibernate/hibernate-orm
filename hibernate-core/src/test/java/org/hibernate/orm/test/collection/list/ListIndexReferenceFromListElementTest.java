/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.list;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Test initially developed for HHH-9195
 *
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				ListIndexReferenceFromListElementTest.LocalOrder.class,
				ListIndexReferenceFromListElementTest.LocalLineItem.class
		}
)
@SessionFactory
public class ListIndexReferenceFromListElementTest {

	@BeforeEach
	public void before(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					LocalOrder localOrder = new LocalOrder();
					localOrder.makeLineItem( "Shoes" );
					localOrder.makeLineItem( "Socks" );
					session.persist( localOrder );
				}
		);
	}

	@AfterEach
	public void after(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					LocalOrder order = session.byId( LocalOrder.class ).load( 1 );
					assertEquals( 2, order.lineItems.size() );
					LocalLineItem shoes = order.lineItems.get( 0 );
					LocalLineItem socks = order.lineItems.get( 1 );
					assertEquals( "Shoes", shoes.name );
					assertEquals( 0, shoes.position );
					assertEquals( 1, socks.position );
					order.lineItems.remove( socks );
					order.lineItems.add( 0, socks );
				}
		);

		scope.inTransaction(
				session -> {
					LocalOrder order = session.byId( LocalOrder.class ).load( 1 );
					assertEquals( 2, order.lineItems.size() );
					LocalLineItem socks = order.lineItems.get( 0 );
					LocalLineItem shoes = order.lineItems.get( 1 );
					assertEquals( "Shoes", shoes.name );
					assertEquals( 0, socks.position );
					assertEquals( 1, shoes.position );
				}
		);
	}

	@Entity(name = "LocalOrder")
	@Table(name = "LocalOrder")
	public static class LocalOrder {
		@Id
		@GeneratedValue
		public Integer id;
		@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@OrderColumn(name = "position")
		public List<LocalLineItem> lineItems = new ArrayList<>();

		public LocalOrder() {
		}

		public LocalLineItem makeLineItem(String name) {
			LocalLineItem lineItem = new LocalLineItem( name, this );
			lineItems.add( lineItem );
			return lineItem;
		}
	}

	@Entity(name = "LocalLineItem")
	@Table(name = "LocalLineItem")
	public static class LocalLineItem {
		@Id
		@GeneratedValue
		public Integer id;
		public String name;
		@ManyToOne
		@JoinColumn
		public LocalOrder order;
		@Column(insertable = false, updatable = false)
		public int position;

		public LocalLineItem() {
		}

		public LocalLineItem(String name, LocalOrder order) {
			this.name = name;
			this.order = order;
		}
	}
}
