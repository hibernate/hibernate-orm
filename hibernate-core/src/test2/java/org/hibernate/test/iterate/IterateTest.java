/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.iterate;
import java.util.Iterator;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class IterateTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "iterate/Item.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "foo" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testIterate() throws Exception {
		sessionFactory().getStatistics().clear();
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Item i1 = new Item("foo");
		Item i2 = new Item("bar");
		s.persist("Item", i1);
		s.persist("Item", i2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Iterator iter = s.getNamedQuery("Item.nameDesc").iterate();
		i1 = (Item) iter.next();
		i2 = (Item) iter.next();
		assertFalse( Hibernate.isInitialized(i1) );
		assertFalse( Hibernate.isInitialized(i2) );
		i1.getName();
		i2.getName();
		assertFalse( Hibernate.isInitialized(i1) );
		assertFalse( Hibernate.isInitialized(i2) );
		assertEquals( i1.getName(), "foo" );
		assertEquals( i2.getName(), "bar" );
		Hibernate.initialize(i1);
		assertFalse( iter.hasNext() );
		s.delete(i1);
		s.delete(i2);
		t.commit();
		s.close();
		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 2 );
	}

	@Test
	public void testScroll() throws Exception {
		sessionFactory().getStatistics().clear();
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Item i1 = new Item("foo");
		Item i2 = new Item("bar");
		s.persist("Item", i1);
		s.persist("Item", i2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		ScrollableResults sr = s.getNamedQuery("Item.nameDesc").scroll();
		assertTrue( sr.next() );
		i1 = (Item) sr.get(0);
		assertTrue( sr.next() );
		i2 = (Item) sr.get(0);
		assertTrue( Hibernate.isInitialized(i1) );
		assertTrue( Hibernate.isInitialized(i2) );
		assertEquals( i1.getName(), "foo" );
		assertEquals( i2.getName(), "bar" );
		assertFalse( sr.next() );
		s.delete(i1);
		s.delete(i2);
		t.commit();
		s.close();
		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );
	}
}

