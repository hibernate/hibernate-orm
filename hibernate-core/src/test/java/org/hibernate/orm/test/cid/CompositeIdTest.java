/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cid/Customer.hbm.xml",
				"org/hibernate/orm/test/cid/Order.hbm.xml",
				"org/hibernate/orm/test/cid/LineItem.hbm.xml",
				"org/hibernate/orm/test/cid/Product.hbm.xml"
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class CompositeIdTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "from LineItem ol where ol.order.id.customerId = 'C111'", LineItem.class ).list()
		);
	}

	@Test
	public void testCompositeIds(SessionFactoryScope scope) {
		Product p = new Product();
		p.setProductId( "A123" );
		p.setDescription( "nipple ring" );
		p.setPrice( new BigDecimal( "1.0" ) );
		p.setNumberAvailable( 1004 );

		Product p2 = new Product();
		p2.setProductId( "X525" );
		p2.setDescription( "nose stud" );
		p2.setPrice( new BigDecimal( "3.0" ) );
		p2.setNumberAvailable( 105 );

		scope.inTransaction(
				session -> {
					session.persist( p );
					session.persist( p2 );

					Customer c = new Customer();
					c.setAddress( "St Kilda Rd, MEL, 3000" );
					c.setName( "Virginia" );
					c.setCustomerId( "C111" );
					session.persist( c );

					Order o = new Order( c );
					o.setOrderDate( Calendar.getInstance() );
					LineItem li = new LineItem( o, p );
					li.setQuantity( 2 );
				}
		);

		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					Order o = session.get( Order.class, new Order.Id( "C111", 0 ) );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
					assertEquals( o.getTotal().intValue(), 2 );
					o.getCustomer().getName();
				}
		);

		statementInspector.clear();
		scope.inTransaction(
				session -> {
					session.createQuery(
							"from Customer c left join fetch c.orders o left join fetch o.lineItems li left join fetch li.product p" )
							.list();
					statementInspector.assertExecutedCount( 1 );
				}
		);

		statementInspector.clear();
		scope.inTransaction(
				session -> {
					session.createQuery( "from Order o left join fetch o.lineItems li left join fetch li.product p" )
							.list();
					statementInspector.assertExecutedCount( 1 );
				}
		);

		statementInspector.clear();
		scope.inTransaction(
				session -> {
					Iterator iter = session.createQuery( "select o.id, li.id from Order o join o.lineItems li" )
							.list()
							.iterator();
					statementInspector.assertExecutedCount( 1 );
					while ( iter.hasNext() ) {
						Object[] stuff = (Object[]) iter.next();
						assertTrue( stuff.length == 2 );
					}
					statementInspector.assertExecutedCount( 1 );
					statementInspector.clear();
					iter = session.createQuery( "from Order o join o.lineItems li", Order.class ).list().iterator();
					statementInspector.assertExecutedCount( 2 );
					statementInspector.clear();
					while ( iter.hasNext() ) {
						Order order = (Order) iter.next();
						assertTrue( Hibernate.isInitialized( order.getLineItems() ) );
					}
					statementInspector.assertExecutedCount( 0 );
				}
		);

		statementInspector.clear();
		scope.inTransaction(
				session -> {
					Customer c = session.get( Customer.class, "C111" );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 0 );

					statementInspector.clear();
					Order o2 = new Order( c );
					o2.setOrderDate( Calendar.getInstance() );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );

					statementInspector.clear();
					session.flush();
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertIsInsert( 0 );


					statementInspector.clear();
					LineItem li2 = new LineItem( o2, p2 );
					li2.setQuantity( 5 );

					List bigOrders = session.createQuery( "from Order o where o.total>10.0" ).list();
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertIsInsert( 0 );
					statementInspector.assertIsSelect( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 0 );

					assertEquals( bigOrders.size(), 1 );
				}
		);
	}

	@Test
	public void testNonLazyFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product p = new Product();
					p.setProductId( "A123" );
					p.setDescription( "nipple ring" );
					p.setPrice( new BigDecimal( "1.0" ) );
					p.setNumberAvailable( 1004 );
					session.persist( p );

					Product p2 = new Product();
					p2.setProductId( "X525" );
					p2.setDescription( "nose stud" );
					p2.setPrice( new BigDecimal( "3.0" ) );
					p2.setNumberAvailable( 105 );
					session.persist( p2 );

					Customer c = new Customer();
					c.setAddress( "St Kilda Rd, MEL, 3000" );
					c.setName( "Virginia" );
					c.setCustomerId( "C111" );
					session.persist( c );

					Order o = new Order( c );
					o.setOrderDate( Calendar.getInstance() );
					LineItem li = new LineItem( o, p );
					li.setQuantity( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					Order o = session.get( Order.class, new Order.Id( "C111", 0 ) );
					assertEquals( o.getTotal().intValue(), 2 );
					o.getCustomer().getName();
				}
		);

		scope.inTransaction(
				session -> {
					Order o = (Order) session.createQuery(
							"from Order o left join fetch o.lineItems li left join fetch li.product p" )
							.uniqueResult();
					assertTrue( Hibernate.isInitialized( o.getLineItems() ) );
					LineItem li = (LineItem) o.getLineItems().iterator().next();
					assertTrue( Hibernate.isInitialized( li ) );
					assertTrue( Hibernate.isInitialized( li.getProduct() ) );
				}
		);

		scope.inTransaction(
				session -> {
					Order o = session.createQuery( "from Order o", Order.class ).uniqueResult();
					assertTrue( Hibernate.isInitialized( o.getLineItems() ) );
					LineItem li = (LineItem) o.getLineItems().iterator().next();
					assertTrue( Hibernate.isInitialized( li ) );
					assertFalse( Hibernate.isInitialized( li.getProduct() ) );
				}
		);
	}

	@Test
	public void testMultipleCollectionFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product p = new Product();
					p.setProductId( "A123" );
					p.setDescription( "nipple ring" );
					p.setPrice( new BigDecimal( "1.0" ) );
					p.setNumberAvailable( 1004 );
					session.persist( p );

					Product p2 = new Product();
					p2.setProductId( "X525" );
					p2.setDescription( "nose stud" );
					p2.setPrice( new BigDecimal( "3.0" ) );
					p2.setNumberAvailable( 105 );
					session.persist( p2 );

					Customer c = new Customer();
					c.setAddress( "St Kilda Rd, MEL, 3000" );
					c.setName( "Virginia" );
					c.setCustomerId( "C111" );
					session.persist( c );

					Order o = new Order( c );
					o.setOrderDate( Calendar.getInstance() );
					LineItem li = new LineItem( o, p );
					li.setQuantity( 2 );
					LineItem li2 = new LineItem( o, p2 );
					li2.setQuantity( 3 );

					Order o2 = new Order( c );
					o2.setOrderDate( Calendar.getInstance() );
					LineItem li3 = new LineItem( o2, p );
					li3.setQuantity( 1 );
					LineItem li4 = new LineItem( o2, p2 );
					li4.setQuantity( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					Customer c = (Customer) session.createQuery(
							"from Customer c left join fetch c.orders o left join fetch o.lineItems li left join fetch li.product p" )
							.uniqueResult();
					assertTrue( Hibernate.isInitialized( c.getOrders() ) );
					assertEquals( c.getOrders().size(), 2 );

					Order o1 = (Order) c.getOrders().get( 0 );
					assertTrue( Hibernate.isInitialized( o1.getLineItems() ) );

					Order o2 = (Order) c.getOrders().get( 1 );
					assertTrue( Hibernate.isInitialized( o2.getLineItems() ) );

					assertEquals( 2, o1.getLineItems().size() );
					assertEquals( 2, o2.getLineItems().size() );
				}
		);
	}
}
