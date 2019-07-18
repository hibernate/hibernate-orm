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
import org.hibernate.exception.SQLGrammarException;

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
		iter = s.createQuery("from Order o join o.lineItems li").list().iterator();
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

