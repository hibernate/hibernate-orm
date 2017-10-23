/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hqlfetchscroll;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HQLScrollFetchTest extends BaseCoreFunctionalTestCase {
	private static final String QUERY = "select p from Parent p join fetch p.children c";

	@Test
	public void testNoScroll() {
		Session s = openSession();
		s.beginTransaction();
		List list = s.createQuery( QUERY ).setResultTransformer( DistinctRootEntityResultTransformer.INSTANCE ).list();
		assertResultFromAllUsers( list );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testScroll() {
		Session s = openSession();
		s.beginTransaction();
		ScrollableResults results = s.createQuery( QUERY + " order by p.name asc, c.name asc" ).scroll();
		List list = new ArrayList();
		while ( results.next() ) {
			list.add( results.get( 0 ) );
		}
		assertResultFromAllUsers( list );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testIncompleteScrollFirstResult() {
		Session s = openSession();
		s.beginTransaction();
		ScrollableResults results = s.createQuery( QUERY + " order by p.name asc" ).scroll();
		results.next();
		Parent p = (Parent) results.get( 0 );
		assertResultFromOneUser( p );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1283" )
	public void testIncompleteScrollSecondResult() {
		Session s = openSession();
		s.beginTransaction();
		ScrollableResults results = s.createQuery( QUERY + " order by p.name asc" ).scroll();
		results.next();
		Parent p = (Parent) results.get( 0 );
		assertResultFromOneUser( p );
		results.next();
		p = (Parent) results.get( 0 );
		assertResultFromOneUser( p );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testIncompleteScrollFirstResultInTransaction() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		ScrollableResults results = s.createQuery( QUERY + " order by p.name asc" ).scroll();
		results.next();
		Parent p = (Parent) results.get( 0 );
		assertResultFromOneUser( p );
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1283" )
	public void testIncompleteScrollSecondResultInTransaction() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		ScrollableResults results = s.createQuery( QUERY + " order by p.name asc" ).scroll();
		results.next();
		Parent p = (Parent) results.get( 0 );
		assertResultFromOneUser( p );
		results.next();
		p = (Parent) results.get( 0 );
		assertResultFromOneUser( p );
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1283")
	public void testIncompleteScroll() {
		Session s = openSession();
		s.beginTransaction();
		ScrollableResults results = s.createQuery( QUERY + " order by p.name asc" ).scroll();
		results.next();
		Parent p = (Parent) results.get( 0 );
		assertResultFromOneUser( p );
		// get the other parent entity from the persistence context along with its first child
		// retrieved from the resultset.
		Parent pOther = null;
		Child cOther = null;
		for ( Object entity : ( (SessionImplementor) s ).getPersistenceContext().getEntitiesByKey().values() ) {
			if ( Parent.class.isInstance( entity ) ) {
				if ( entity != p ) {
					if ( pOther != null ) {
						fail( "unexpected parent found." );
					}
					pOther = (Parent) entity;
				}
			}
			else if ( Child.class.isInstance( entity ) ) {
				if ( ! p.getChildren().contains( entity ) ) {
					if ( cOther != null ) {
						fail( "unexpected child entity found" );
					}
					cOther = (Child) entity;
				}
			}
			else {
				fail( "unexpected type of entity." );
			}
		}
		// check that the same second parent is obtained by calling Session.get()
		assertNull( pOther );
		assertNull( cOther );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1283" )
	public void testIncompleteScrollLast() {
		Session s = openSession();
		s.beginTransaction();
		ScrollableResults results = s.createQuery( QUERY + " order by p.name asc" ).scroll();
		results.next();
		Parent p = (Parent) results.get( 0 );
		assertResultFromOneUser( p );
		results.last();
		// get the other parent entity from the persistence context.
		// since the result set was scrolled to the end, the other parent entity's collection has been
		// properly initialized.
		Parent pOther = null;
		Set childrenOther = new HashSet();
		for ( Object entity : ( ( SessionImplementor) s ).getPersistenceContext().getEntitiesByKey().values() ) {
			if ( Parent.class.isInstance( entity ) ) {
				if ( entity != p ) {
					if ( pOther != null ) {
						fail( "unexpected parent found." );
					}
					pOther = (Parent) entity;
				}
			}
			else if ( Child.class.isInstance( entity ) ) {
				if ( ! p.getChildren().contains( entity ) ) {
					childrenOther.add( entity );
				}
			}
			else {
				fail( "unexpected type of entity." );
			}
		}
		// check that the same second parent is obtained by calling Session.get()
		assertNotNull( pOther );
		assertSame( pOther, s.get( Parent.class, pOther.getId() ) );
		// access pOther's collection; should be completely loaded
		assertTrue( Hibernate.isInitialized( pOther.getChildren() ) );
		assertEquals( childrenOther, pOther.getChildren() );
		assertResultFromOneUser( pOther );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1283" )
	public void testScrollOrderParentAsc() {
		Session s = openSession();
		s.beginTransaction();
		ScrollableResults results = s.createQuery( QUERY + " order by p.name asc" ).scroll();
		List list = new ArrayList();
		while ( results.next() ) {
			list.add( results.get( 0 ) );
		}
		assertResultFromAllUsers( list );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1283" )
	public void testScrollOrderParentDesc() {
		Session s = openSession();
		s.beginTransaction();
		ScrollableResults results = s.createQuery( QUERY + " order by p.name desc" ).scroll();
		List list = new ArrayList();
		while ( results.next() ) {
			list.add( results.get( 0 ) );
		}
		assertResultFromAllUsers( list );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1283" )
	public void testScrollOrderParentAscChildrenAsc() {
		Session s = openSession();
		s.beginTransaction();
		ScrollableResults results = s.createQuery( QUERY + " order by p.name asc, c.name asc" ).scroll();
		List list = new ArrayList();
		while ( results.next() ) {
			list.add( results.get( 0 ) );
		}
		assertResultFromAllUsers( list );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1283" )
	public void testScrollOrderParentAscChildrenDesc() {
		Session s = openSession();
		s.beginTransaction();
		ScrollableResults results = s.createQuery( QUERY + " order by p.name asc, c.name desc" ).scroll();
		List list = new ArrayList();
		while ( results.next() ) {
			list.add( results.get( 0 ) );
		}
		assertResultFromAllUsers( list );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testScrollOrderChildrenDesc() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p0 = new Parent( "parent0" );
		s.save( p0 );
		t.commit();
		s.close();
		s = openSession();
		ScrollableResults results = s.createQuery( QUERY + " order by c.name desc" ).scroll();
		List list = new ArrayList();
		while ( results.next() ) {
			list.add( results.get( 0 ) );
		}
		try {
			assertResultFromAllUsers( list );
			fail( "should have failed because data is ordered incorrectly." );
		}
		catch ( AssertionError ex ) {
			// expected
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testListOrderChildrenDesc() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p0 = new Parent( "parent0" );
		s.save( p0 );
		t.commit();
		s.close();
		s = openSession();
		List results = s.createQuery( QUERY + " order by c.name desc" ).list();
		try {
			assertResultFromAllUsers( results );
			fail( "should have failed because data is ordered incorrectly." );
		}
		catch ( AssertionError ex ) {
			// expected
		}
		finally {
			s.close();
		}
	}

	private void assertResultFromOneUser(Parent parent) {
		assertEquals(
					"parent " + parent + " has incorrect collection(" + parent.getChildren() + ").",
					3,
					parent.getChildren().size()
		);
	}

	private void assertResultFromAllUsers(List list) {
		assertEquals( "list is not correct size: ", 2, list.size() );
		for ( Object aList : list ) {
			assertResultFromOneUser( (Parent) aList );
		}
	}

	@Override
	protected void prepareTest() throws Exception {
	    Session s = openSession();
	    Transaction t = s.beginTransaction();
	    Child child_1_1 = new Child( "achild1-1");
	    Child child_1_2 = new Child( "ychild1-2");
	    Child child_1_3 = new Child( "dchild1-3");
	    Child child_2_1 = new Child( "bchild2-1");
	    Child child_2_2 = new Child( "cchild2-2");
	    Child child_2_3 = new Child( "zchild2-3");
	
	    s.save( child_1_1 );
	    s.save( child_2_1 );
	    s.save( child_1_2 );
	    s.save( child_2_2 );
	    s.save( child_1_3 );
	    s.save( child_2_3 );
	
	    s.flush();
	
	    Parent p1 = new Parent( "parent1" );
	    p1.addChild( child_1_1 );
	    p1.addChild( child_1_2 );
	    p1.addChild( child_1_3 );
	    s.save( p1 );
	
	    Parent p2 = new Parent( "parent2" );
	    p2.addChild( child_2_1 );
	    p2.addChild( child_2_2 );
	    p2.addChild( child_2_3 );
	    s.save( p2 );
	
	    t.commit();
	    s.close();
	}
	
	@Override
	protected void cleanupTest() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		List list = s.createQuery( "from Parent" ).list();
		for ( Iterator i = list.iterator(); i.hasNext(); ) {
			s.delete( i.next() );
		}
		t.commit();
		s.close();
	}

	public String[] getMappings() {
		return new String[] { "hqlfetchscroll/ParentChild.hbm.xml" };
	}
}
