//$Id: BatchFetchTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.batchfetch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class BatchFetchTest extends FunctionalTestCase {

	public BatchFetchTest(String str) {
		super( str );
	}

	public String[] getMappings() {
		return new String[] { "batchfetch/ProductLine.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( BatchFetchTest.class );
	}

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

		s.getSessionFactory().evict( Model.class );
		s.getSessionFactory().evict( ProductLine.class );

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
		for ( Iterator i = list.iterator(); i.hasNext(); ) {
			assertFalse( Hibernate.isInitialized( ( ( Model ) i.next() ).getProductLine() ) );
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

