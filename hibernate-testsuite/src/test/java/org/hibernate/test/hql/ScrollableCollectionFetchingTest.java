// $Id: ScrollableCollectionFetchingTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.hql;

import junit.framework.Test;

import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Tests the new functionality of allowing scrolling of results which
 * contain collection fetches.
 *
 * @author Steve Ebersole
 */
public class ScrollableCollectionFetchingTest extends FunctionalTestCase {

	public ScrollableCollectionFetchingTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ScrollableCollectionFetchingTest.class );
	}

	public String[] getMappings() {
		return new String[] { "hql/Animal.hbm.xml" };
	}

	public void testTupleReturnFails() {
		Session s = openSession();
		Transaction txn = s.beginTransaction();

		try {
			s.createQuery( "select a, a.weight from Animal a inner join fetch a.offspring" ).scroll();
			fail( "scroll allowed with collection fetch and reurning tuples" );
		}
		catch( HibernateException e ) {
			// expected result...
		}

		txn.commit();
		s.close();
	}

	public void testScrollingJoinFetchesEmptyResultSet() {
		Session s = openSession();
		Transaction txn = s.beginTransaction();

		final String query = "from Animal a left join fetch a.offspring where a.description like :desc order by a.id";

		// first, as a control, make sure there are no results
		int size = s.createQuery( query ).setString( "desc", "root%" ).list().size();
		assertEquals( 0, size );

		// now get the scrollable results
		ScrollableResults results = s.createQuery( query ).setString( "desc", "root%" ).scroll();

		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );

		assertFalse( results.next() );
		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );

		assertFalse( results.previous() );
		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );

		results.beforeFirst();
		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );
		assertFalse( results.next() );

		assertFalse( results.first() );
		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );
		assertFalse( results.next() );

		results.afterLast();
		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );
		assertFalse( results.next() );

		assertFalse( results.last() );
		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );
		assertFalse( results.next() );

		for ( int i=1; i<3; i++ ) {
			assertFalse( results.scroll( i ) );
			assertFalse( results.isFirst() );
			assertFalse( results.isLast() );

			assertFalse( results.scroll( - i ) );
			assertFalse( results.isFirst() );
			assertFalse( results.isLast() );

			assertFalse( results.setRowNumber( i ) );
			assertFalse( results.isFirst() );
			assertFalse( results.isLast() );

			assertFalse( results.setRowNumber( - i ) );
			assertFalse( results.isFirst() );
			assertFalse( results.isLast() );
		}

		txn.commit();
		s.close();
	}

	public void testScrollingJoinFetchesSingleRowResultSet() {
		Session s = openSession();
		Transaction txn = s.beginTransaction();

		Animal mother = new Animal();
		mother.setDescription( "root-1" );

		Animal daughter = new Animal();
		daughter.setDescription( "daughter" );

		daughter.setMother( mother );
		mother.addOffspring( daughter );

		s.save( mother );
		s.save( daughter );

		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();

		assertNotNull(s
		        .createQuery( "from Animal a left join fetch a.offspring where a.description like :desc order by a.id" )
		        .setString( "desc", "root%" )
		        .uniqueResult() );

		ScrollableResults results = s
		        .createQuery( "from Animal a left join fetch a.offspring where a.description like :desc order by a.id" )
		        .setString( "desc", "root%" )
		        .scroll();

		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );
		assertFalse( results.previous() );		

		assertTrue( results.next() );
		assertTrue( results.isFirst() );
		assertTrue( results.isLast() );

		assertFalse( results.next() );
		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );

		assertTrue( results.previous() );
		assertTrue( results.isFirst() );
		assertTrue( results.isLast() );

		assertFalse( results.previous() );
		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );

		assertTrue( results.next() );
		assertTrue( results.isFirst() );
		assertTrue( results.isLast() );

		results.beforeFirst();
		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );
		assertFalse( results.previous() );

		assertTrue( results.first() );
		assertTrue( results.isFirst() );
		assertTrue( results.isLast() );
		assertFalse( results.next() );

		results.afterLast();
		assertFalse( results.isFirst() );
		assertFalse( results.isLast() );
		assertFalse( results.next() );

		assertTrue( results.last() );
		assertTrue( results.isFirst() );
		assertTrue( results.isLast() );
		assertFalse( results.next() );

		assertTrue( results.first() );
		assertTrue( results.isFirst() );
		assertTrue( results.isLast() );		

		for ( int i=1; i<3; i++ ) {
			assertTrue( results.setRowNumber( 1 ) );
			assertTrue( results.isFirst() );
			assertTrue( results.isLast() );

			assertFalse( results.scroll( i ) );
			assertFalse( results.isFirst() );
			assertFalse( results.isLast() );

			assertTrue( results.setRowNumber( 1 ) );
			assertTrue( results.isFirst() );
			assertTrue( results.isLast() );

			assertFalse( results.scroll( - i ) );
			assertFalse( results.isFirst() );
			assertFalse( results.isLast() );

			if ( i != 1 ) {
				assertFalse( results.setRowNumber( i ) );
				assertFalse( results.isFirst() );
				assertFalse( results.isLast() );

				assertFalse( results.setRowNumber( - i ) );
				assertFalse( results.isFirst() );
				assertFalse( results.isLast() );
			}
		}

		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();

		s.createQuery( "delete Animal where not description like 'root%'" ).executeUpdate();
		s.createQuery( "delete Animal" ).executeUpdate();

		txn.commit();
		s.close();
	}

	public void testScrollingJoinFetchesForward() {
		if ( ! supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() ) {
			return;
		}

		TestData data = new TestData();
		data.prepare();

		Session s = openSession();
		Transaction txn = s.beginTransaction();

		ScrollableResults results = s
		        .createQuery( "from Animal a left join fetch a.offspring where a.description like :desc order by a.id" )
		        .setString( "desc", "root%" )
				.scroll( ScrollMode.FORWARD_ONLY );

		int counter = 0;
		while ( results.next() ) {
			counter++;
			Animal animal = ( Animal ) results.get( 0 );
			checkResult( animal );
		}
		assertEquals( "unexpected result count", 2, counter );

		txn.commit();
		s.close();

		data.cleanup();
	}

	public void testScrollingJoinFetchesReverse() {
		TestData data = new TestData();
		data.prepare();

		Session s = openSession();
		Transaction txn = s.beginTransaction();

		ScrollableResults results = s
		        .createQuery( "from Animal a left join fetch a.offspring where a.description like :desc order by a.id" )
		        .setString( "desc", "root%" )
		        .scroll();

		results.afterLast();

		int counter = 0;
		while ( results.previous() ) {
			counter++;
			Animal animal = ( Animal ) results.get( 0 );
			checkResult( animal );
		}
		assertEquals( "unexpected result count", 2, counter );

		txn.commit();
		s.close();

		data.cleanup();
	}

	public void testScrollingJoinFetchesPositioning() {
		TestData data = new TestData();
		data.prepare();

		Session s = openSession();
		Transaction txn = s.beginTransaction();

		ScrollableResults results = s
		        .createQuery( "from Animal a left join fetch a.offspring where a.description like :desc order by a.id" )
		        .setString( "desc", "root%" )
		        .scroll();

		results.first();
		Animal animal = ( Animal ) results.get( 0 );
		assertEquals( "first() did not return expected row", data.root1Id, animal.getId() );

		results.scroll( 1 );
		animal = ( Animal ) results.get( 0 );
		assertEquals( "scroll(1) did not return expected row", data.root2Id, animal.getId() );

		results.scroll( -1 );
		animal = ( Animal ) results.get( 0 );
		assertEquals( "scroll(-1) did not return expected row", data.root1Id, animal.getId() );

		results.setRowNumber( 1 );
		animal = ( Animal ) results.get( 0 );
		assertEquals( "setRowNumber(1) did not return expected row", data.root1Id, animal.getId() );

		results.setRowNumber( 2 );
		animal = ( Animal ) results.get( 0 );
		assertEquals( "setRowNumber(2) did not return expected row", data.root2Id, animal.getId() );

		txn.commit();
		s.close();

		data.cleanup();
	}

	private void checkResult(Animal animal) {
		if ( "root-1".equals( animal.getDescription() ) ) {
			assertEquals( "root-1 did not contain both children", 2, animal.getOffspring().size() );
		}
		else if ( "root-2".equals( animal.getDescription() ) ) {
			assertEquals( "root-2 did not contain zero children", 0, animal.getOffspring().size() );
		}
	}

	private class TestData {

		private Long root1Id;
		private Long root2Id;

		private void prepare() {
			Session s = openSession();
			Transaction txn = s.beginTransaction();

			Animal mother = new Animal();
			mother.setDescription( "root-1" );

			Animal another = new Animal();
			another.setDescription( "root-2" );

			Animal son = new Animal();
			son.setDescription( "son");

			Animal daughter = new Animal();
			daughter.setDescription( "daughter" );

			Animal grandson = new Animal();
			grandson.setDescription( "grandson" );

			Animal grandDaughter = new Animal();
			grandDaughter.setDescription( "granddaughter" );

			son.setMother( mother );
			mother.addOffspring( son );

			daughter.setMother( mother );
			mother.addOffspring( daughter );

			grandson.setMother( daughter );
			daughter.addOffspring( grandson );

			grandDaughter.setMother( daughter );
			daughter.addOffspring( grandDaughter );

			s.save( mother );
			s.save( another );
			s.save( son );
			s.save( daughter );
			s.save( grandson );
			s.save( grandDaughter );

			txn.commit();
			s.close();

			root1Id = mother.getId();
			root2Id = another.getId();
		}

		private void cleanup() {
			Session s = openSession();
			Transaction txn = s.beginTransaction();
			
			s.createQuery( "delete Animal where description like 'grand%'" ).executeUpdate();
			s.createQuery( "delete Animal where not description like 'root%'" ).executeUpdate();
			s.createQuery( "delete Animal" ).executeUpdate();

			txn.commit();
			s.close();
		}
	}
}
