/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.list;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Test initially developed for HHH-9195
 * @author Steve Ebersole
 */
public class ListIndexReferenceFromListElementTest extends BaseNonConfigCoreFunctionalTestCase {
	@Entity( name = "LocalOrder" )
	@Table( name = "LocalOrder" )
	public static class LocalOrder {
		@Id
		@GeneratedValue
		public Integer id;
		@OneToMany( mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
		@OrderColumn( name = "position" )
		public List<LocalLineItem> lineItems = new ArrayList<LocalLineItem>();

		public LocalOrder() {
		}

		public LocalLineItem makeLineItem(String name) {
			LocalLineItem lineItem = new LocalLineItem( name, this );
			lineItems.add( lineItem );
			return lineItem;
		}
	}

	@Entity( name = "LocalLineItem" )
	@Table( name = "LocalLineItem" )
	public static class LocalLineItem {
		@Id
		@GeneratedValue
		public Integer id;
		public String name;
		@ManyToOne
		@JoinColumn
		public LocalOrder order;
		@Column( insertable = false, updatable = false )
		public int position;

		public LocalLineItem() {
		}

		public LocalLineItem(String name, LocalOrder order) {
			this.name = name;
			this.order = order;
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { LocalOrder.class, LocalLineItem.class };
	}

	@Before
	public void before() {
		Session s = sessionFactory().openSession();
		s.beginTransaction();
		LocalOrder localOrder = new LocalOrder();
		localOrder.makeLineItem( "Shoes" );
		localOrder.makeLineItem( "Socks" );
		s.save( localOrder );
		s.getTransaction().commit();
		s.close();
	}

	@After
	public void after() {
		Session s = sessionFactory().openSession();
		s.beginTransaction();
		s.createQuery( "delete LocalLineItem" ).executeUpdate();
		s.createQuery( "delete LocalOrder" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testIt() {
		{
			Session s = openSession();
			s.beginTransaction();
			LocalOrder order = s.byId( LocalOrder.class ).load( 1 );
			assertEquals( 2, order.lineItems.size() );
			LocalLineItem shoes = order.lineItems.get( 0 );
			LocalLineItem socks = order.lineItems.get( 1 );
			assertEquals( "Shoes", shoes.name );
			assertEquals( 0, shoes.position );
			assertEquals( 1, socks.position );
			order.lineItems.remove( socks );
			order.lineItems.add( 0, socks );
			s.getTransaction().commit();
			s.close();
		}

		{
			Session s = openSession();
			s.beginTransaction();
			LocalOrder order = s.byId( LocalOrder.class ).load( 1 );
			assertEquals( 2, order.lineItems.size() );
			LocalLineItem socks = order.lineItems.get( 0 );
			LocalLineItem shoes = order.lineItems.get( 1 );
			assertEquals( "Shoes", shoes.name );
			assertEquals( 0, socks.position );
			assertEquals( 1, shoes.position );

			s.getTransaction().commit();
			s.close();
		}
	}
}
