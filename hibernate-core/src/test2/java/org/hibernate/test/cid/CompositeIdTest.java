/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.PersistenceException;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.hql.spi.QueryTranslator;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Gavin King
 */
public class CompositeIdTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "cid/Customer.hbm.xml", "cid/Order.hbm.xml", "cid/LineItem.hbm.xml", "cid/Product.hbm.xml" };
	}

	@Test
	public void testNonDistinctCountOfEntityWithCompositeId() {
		// the check here is all based on whether we had commas in the expressions inside the count
		final HQLQueryPlan plan = sessionFactory().getQueryPlanCache().getHQLQueryPlan(
				"select count(o) from Order o",
				false,
				Collections.EMPTY_MAP
		);
		assertEquals( 1, plan.getTranslators().length );
		final QueryTranslator translator = plan.getTranslators()[0];
		final String generatedSql = translator.getSQLString();

		final int countExpressionListStart = generatedSql.indexOf( "count(" );
		final int countExpressionListEnd = generatedSql.indexOf( ")", countExpressionListStart );
		final String countExpressionFragment = generatedSql.substring( countExpressionListStart+6, countExpressionListEnd+1 );
		final boolean hadCommas = countExpressionFragment.contains( "," );

		// set up the expectation based on Dialect...
		final boolean expectCommas = sessionFactory().getDialect().supportsTupleCounts();

		assertEquals( expectCommas, hadCommas );
	}

	@Test
	@SkipForDialect(value = Oracle8iDialect.class, comment = "Cannot count distinct over multiple columns in Oracle")
	@SkipForDialect(value = SQLServerDialect.class, comment = "Cannot count distinct over multiple columns in SQL Server")
	public void testDistinctCountOfEntityWithCompositeId() {
		// today we do not account for Dialects supportsTupleDistinctCounts() is false.  though really the only
		// "option" there is to throw an error.
		final HQLQueryPlan plan = sessionFactory().getQueryPlanCache().getHQLQueryPlan(
				"select count(distinct o) from Order o",
				false,
				Collections.EMPTY_MAP
		);
		assertEquals( 1, plan.getTranslators().length );
		final QueryTranslator translator = plan.getTranslators()[0];
		final String generatedSql = translator.getSQLString();
		System.out.println( "Generated SQL : " + generatedSql );

		final int countExpressionListStart = generatedSql.indexOf( "count(" );
		final int countExpressionListEnd = generatedSql.indexOf( ")", countExpressionListStart );
		final String countExpressionFragment = generatedSql.substring( countExpressionListStart+6, countExpressionListEnd+1 );
		assertTrue( countExpressionFragment.startsWith( "distinct" ) );
		assertTrue( countExpressionFragment.contains( "," ) );
		
		Session s = openSession();
		s.beginTransaction();
		Customer c = new Customer();
		c.setCustomerId( "1" );
		c.setAddress("123 somewhere");
		c.setName("Brett");
		Order o1 = new Order( c );
		o1.setOrderDate( Calendar.getInstance() );
		Order o2 = new Order( c );
		o2.setOrderDate( Calendar.getInstance() );
		s.persist( c );
		s.persist( o1 );
		s.persist( o2 );
		s.getTransaction().commit();
		s.clear();

		s.beginTransaction();
		try {
			long count = ( Long ) s.createQuery( "select count(distinct o) FROM Order o" ).uniqueResult();
			if ( ! getDialect().supportsTupleDistinctCounts() ) {
				fail( "expected PersistenceException caused by SQLGrammarException" );
			}
			assertEquals( 2l, count );
		}
		catch ( PersistenceException e ) {
			if ( ! (e.getCause() instanceof SQLGrammarException) || getDialect().supportsTupleDistinctCounts() ) {
				throw e;
			}
		}
		s.getTransaction().commit();
		s.close();
		
		s = openSession();
		s.beginTransaction();
		s.createQuery("delete from Order").executeUpdate();
		s.createQuery("delete from Customer").executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testQuery() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery("from LineItem ol where ol.order.id.customerId = 'C111'").list();
		t.commit();
		s.close();
	}

	@Test
	public void testCompositeIds() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		
		Product p = new Product();
		p.setProductId("A123");
		p.setDescription("nipple ring");
		p.setPrice( new BigDecimal(1.0) );
		p.setNumberAvailable(1004);
		s.persist(p);
		
		Product p2 = new Product();
		p2.setProductId("X525");
		p2.setDescription("nose stud");
		p2.setPrice( new BigDecimal(3.0) );
		p2.setNumberAvailable(105);
		s.persist(p2);
		
		Customer c = new Customer();
		c.setAddress("St Kilda Rd, MEL, 3000");
		c.setName("Virginia");
		c.setCustomerId("C111");
		s.persist(c);
		
		Order o = new Order(c);
		o.setOrderDate( Calendar.getInstance() );
		LineItem li = new LineItem(o, p);
		li.setQuantity(2);

		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		o = (Order) s.get( Order.class, new Order.Id("C111", 0) );
		assertEquals( o.getTotal().intValue(), 2 );
		o.getCustomer().getName();
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery("from Customer c left join fetch c.orders o left join fetch o.lineItems li left join fetch li.product p").list();
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("from Order o left join fetch o.lineItems li left join fetch li.product p").list();
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		Iterator iter = s.createQuery("select o.id, li.id from Order o join o.lineItems li").list().iterator();
		while ( iter.hasNext() ) {
			Object[] stuff = (Object[]) iter.next();
			assertTrue(stuff.length==2);
		}
		iter = s.createQuery("from Order o join o.lineItems li").iterate();
		while ( iter.hasNext() ) {
			Object[] stuff = (Object[]) iter.next();
			assertTrue(stuff.length==2);
		}
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		c = (Customer) s.get( Customer.class, "C111" );
		Order o2 = new Order(c);
		o2.setOrderDate( Calendar.getInstance() );
		s.flush();
		LineItem li2 = new LineItem(o2, p2);
		li2.setQuantity(5);
		List bigOrders = s.createQuery("from Order o where o.total>10.0").list();
		assertEquals( bigOrders.size(), 1 );
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from LineItem").executeUpdate();
		s.createQuery("delete from Order").executeUpdate();
		s.createQuery("delete from Customer").executeUpdate();
		s.createQuery("delete from Product").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testNonLazyFetch() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		
		Product p = new Product();
		p.setProductId("A123");
		p.setDescription("nipple ring");
		p.setPrice( new BigDecimal(1.0) );
		p.setNumberAvailable(1004);
		s.persist(p);
		
		Product p2 = new Product();
		p2.setProductId("X525");
		p2.setDescription("nose stud");
		p2.setPrice( new BigDecimal(3.0) );
		p2.setNumberAvailable(105);
		s.persist(p2);
		
		Customer c = new Customer();
		c.setAddress("St Kilda Rd, MEL, 3000");
		c.setName("Virginia");
		c.setCustomerId("C111");
		s.persist(c);
		
		Order o = new Order(c);
		o.setOrderDate( Calendar.getInstance() );
		LineItem li = new LineItem(o, p);
		li.setQuantity(2);

		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		o = (Order) s.get( Order.class, new Order.Id("C111", 0) );
		assertEquals( o.getTotal().intValue(), 2 );
		o.getCustomer().getName();
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		o = (Order) s.createQuery("from Order o left join fetch o.lineItems li left join fetch li.product p").uniqueResult();
		assertTrue( Hibernate.isInitialized( o.getLineItems() ) );
		li = (LineItem) o.getLineItems().iterator().next();
		assertTrue( Hibernate.isInitialized( li ) );
		assertTrue( Hibernate.isInitialized( li.getProduct() ) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		o = (Order) s.createQuery("from Order o").uniqueResult();
		assertTrue( Hibernate.isInitialized( o.getLineItems() ) );
		li = (LineItem) o.getLineItems().iterator().next();
		assertTrue( Hibernate.isInitialized( li ) );
		assertFalse( Hibernate.isInitialized( li.getProduct() ) );
		t.commit();
		s.close();
		
		
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from LineItem").executeUpdate();
		s.createQuery("delete from Order").executeUpdate();
		s.createQuery("delete from Customer").executeUpdate();
		s.createQuery("delete from Product").executeUpdate();
		t.commit();
		s.close();
		
	}

	@Test
	public void testMultipleCollectionFetch() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		
		Product p = new Product();
		p.setProductId("A123");
		p.setDescription("nipple ring");
		p.setPrice( new BigDecimal(1.0) );
		p.setNumberAvailable(1004);
		s.persist(p);
		
		Product p2 = new Product();
		p2.setProductId("X525");
		p2.setDescription("nose stud");
		p2.setPrice( new BigDecimal(3.0) );
		p2.setNumberAvailable(105);
		s.persist(p2);
		
		Customer c = new Customer();
		c.setAddress("St Kilda Rd, MEL, 3000");
		c.setName("Virginia");
		c.setCustomerId("C111");
		s.persist(c);
		
		Order o = new Order(c);
		o.setOrderDate( Calendar.getInstance() );
		LineItem li = new LineItem(o, p);
		li.setQuantity(2);
		LineItem li2 = new LineItem(o, p2);
		li2.setQuantity(3);

		Order o2 = new Order(c);
		o2.setOrderDate( Calendar.getInstance() );
		LineItem li3 = new LineItem(o2, p);
		li3.setQuantity(1);
		LineItem li4 = new LineItem(o2, p2);
		li4.setQuantity(1);

		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		c = (Customer) s.createQuery("from Customer c left join fetch c.orders o left join fetch o.lineItems li left join fetch li.product p").uniqueResult();
		assertTrue( Hibernate.isInitialized( c.getOrders() ) );
		assertEquals( c.getOrders().size(), 2 );
		assertTrue( Hibernate.isInitialized( ( (Order) c.getOrders().get(0) ).getLineItems() ) );
		assertTrue( Hibernate.isInitialized( ( (Order) c.getOrders().get(1) ).getLineItems() ) );
		assertEquals( ( (Order) c.getOrders().get(0) ).getLineItems().size(), 2 );
		assertEquals( ( (Order) c.getOrders().get(1) ).getLineItems().size(), 2 );
		t.commit();
		s.close();
				
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from LineItem").executeUpdate();
		s.createQuery("delete from Order").executeUpdate();
		s.createQuery("delete from Customer").executeUpdate();
		s.createQuery("delete from Product").executeUpdate();
		t.commit();
		s.close();
	}

}

