/*
  * JBoss, Home of Professional Open Source
  * Copyright 2012, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */

package org.hibernate.test.ondemandload;

import java.math.BigDecimal;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LazyLoadingTest extends BaseCoreFunctionalTestCase {

	@Before
	public void setUpData() {
		Session s = openSession();
		s.beginTransaction();
		Store store = new Store( 1 )
				.setName( "Acme Super Outlet" );
		s.persist( store );

		Product product = new Product( "007" )
				.setName( "widget" )
				.setDescription( "FooBar" );
		s.persist( product );

		store.addInventoryProduct( product )
				.setQuantity( 10L )
				.setStorePrice( new BigDecimal( 500 ) );

		s.getTransaction().commit();
		s.close();
	}

	@After
	public void cleanUpData() {
		Session s = openSession();
		s.beginTransaction();
		s.delete( s.get( Store.class, 1 ) );
		s.delete( s.get( Product.class, "007" ) );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testLazyCollectionLoadingWithClearedSession() {
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		// first load the store, making sure collection is not initialized
		Store store = (Store) s.get( Store.class, 1 );
		assertNotNull( store );
		assertFalse( Hibernate.isInitialized( store.getInventories() ) );

		assertEquals( 1, sessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 0, sessionFactory().getStatistics().getSessionCloseCount() );

		// then clear session and try to initialize collection
		s.clear();
		store.getInventories().size();
		assertTrue( Hibernate.isInitialized( store.getInventories() ) );

		assertEquals( 2, sessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 1, sessionFactory().getStatistics().getSessionCloseCount() );

		s.clear();
		store = (Store) s.get( Store.class, 1 );
		assertNotNull( store );
		assertFalse( Hibernate.isInitialized( store.getInventories() ) );

		assertEquals( 2, sessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 1, sessionFactory().getStatistics().getSessionCloseCount() );

		s.clear();
		store.getInventories().iterator();
		assertTrue( Hibernate.isInitialized( store.getInventories() ) );

		assertEquals( 3, sessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 2, sessionFactory().getStatistics().getSessionCloseCount() );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testLazyCollectionLoadingWithClosedSession() {
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		// first load the store, making sure collection is not initialized
		Store store = (Store) s.get( Store.class, 1 );
		assertNotNull( store );
		assertFalse( Hibernate.isInitialized( store.getInventories() ) );

		assertEquals( 1, sessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 0, sessionFactory().getStatistics().getSessionCloseCount() );

		// close the session and try to initialize collection
		s.getTransaction().commit();
		s.close();

		assertEquals( 1, sessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 1, sessionFactory().getStatistics().getSessionCloseCount() );

		store.getInventories().size();
		assertTrue( Hibernate.isInitialized( store.getInventories() ) );

		assertEquals( 2, sessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 2, sessionFactory().getStatistics().getSessionCloseCount() );
	}

	@Test
	public void testLazyEntityLoadingWithClosedSession() {
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		// first load the store, making sure it is not initialized
		Store store = (Store) s.load( Store.class, 1 );
		assertNotNull( store );
		assertFalse( Hibernate.isInitialized( store ) );

		assertEquals( 1, sessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 0, sessionFactory().getStatistics().getSessionCloseCount() );

		// close the session and try to initialize store
		s.getTransaction().commit();
		s.close();

		assertEquals( 1, sessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 1, sessionFactory().getStatistics().getSessionCloseCount() );

		store.getName();
		assertTrue( Hibernate.isInitialized( store ) );

		assertEquals( 2, sessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 2, sessionFactory().getStatistics().getSessionCloseCount() );
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Store.class,
				Inventory.class,
				Product.class
		};
	}

}
