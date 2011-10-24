/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.batchfetch;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class BatchFetchTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "batchfetch/ProductLine.hbm.xml" };
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testBatchFetch() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		ProductLine cars = new ProductLine();
		cars.setDescription( "Cars" );
		Model monaro = new Model( cars );
		monaro.setName( "monaro" );
		monaro.setDescription( "Holden Monaro" );
		Model hsv = new Model( cars );
		hsv.setName( "hsv" );
		hsv.setDescription( "Holden Commodore HSV" );
		s.save( cars );

		ProductLine oss = new ProductLine();
		oss.setDescription( "OSS" );
		Model jboss = new Model( oss );
		jboss.setName( "JBoss" );
		jboss.setDescription( "JBoss Application Server" );
		Model hibernate = new Model( oss );
		hibernate.setName( "Hibernate" );
		hibernate.setDescription( "Hibernate" );
		Model cache = new Model( oss );
		cache.setName( "JBossCache" );
		cache.setDescription( "JBoss TreeCache" );
		s.save( oss );

		t.commit();
		s.close();

		s.getSessionFactory().getCache().evictEntityRegion( Model.class );
		s.getSessionFactory().getCache().evictEntityRegion( ProductLine.class );

		s = openSession();
		t = s.beginTransaction();

		List list = s.createQuery( "from ProductLine pl order by pl.description" ).list();
		cars = ( ProductLine ) list.get( 0 );
		oss = ( ProductLine ) list.get( 1 );
		assertFalse( Hibernate.isInitialized( cars.getModels() ) );
		assertFalse( Hibernate.isInitialized( oss.getModels() ) );
		assertEquals( cars.getModels().size(), 2 ); //fetch both collections
		assertTrue( Hibernate.isInitialized( cars.getModels() ) );
		assertTrue( Hibernate.isInitialized( oss.getModels() ) );

		s.clear();

		list = s.createQuery( "from Model m" ).list();
		hibernate = ( Model ) s.get( Model.class, hibernate.getId() );
		hibernate.getProductLine().getId();
		for ( Object aList : list ) {
			assertFalse( Hibernate.isInitialized( ((Model) aList).getProductLine() ) );
		}
		assertEquals( hibernate.getProductLine().getDescription(), "OSS" ); //fetch both productlines

		s.clear();

		Iterator iter = s.createQuery( "from Model" ).iterate();
		list = new ArrayList();
		while ( iter.hasNext() ) {
			list.add( iter.next() );
		}
		Model m = ( Model ) list.get( 0 );
		m.getDescription(); //fetch a batch of 4

		s.clear();

		list = s.createQuery( "from ProductLine" ).list();
		ProductLine pl = ( ProductLine ) list.get( 0 );
		ProductLine pl2 = ( ProductLine ) list.get( 1 );
		s.evict( pl2 );
		pl.getModels().size(); //fetch just one collection! (how can we write an assertion for that??)

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		list = s.createQuery( "from ProductLine pl order by pl.description" ).list();
		cars = ( ProductLine ) list.get( 0 );
		oss = ( ProductLine ) list.get( 1 );
		assertEquals( cars.getModels().size(), 2 );
		assertEquals( oss.getModels().size(), 3 );
		s.delete( cars );
		s.delete( oss );
		t.commit();
		s.close();
	}

}

