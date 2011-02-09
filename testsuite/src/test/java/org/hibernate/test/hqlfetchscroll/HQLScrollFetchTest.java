package org.hibernate.test.hqlfetchscroll;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.transform.DistinctRootEntityResultTransformer;


public class HQLScrollFetchTest extends FunctionalTestCase {
	private static final String QUERY = "select p from Parent p join fetch p.children c";

	public HQLScrollFetchTest(String name) {
		super( name );
	}

	public void testNoScroll() {
		try {
			insertTestData();
			Session s = openSession();
			List list = s.createQuery( QUERY ).setResultTransformer( DistinctRootEntityResultTransformer.INSTANCE ).list();
			assertResultFromAllUsers( list );
			s.close();
		}
		finally {
			deleteAll();
		}
	}

	public void testScroll() {
		try {
			insertTestData();
			Session s = openSession();
			ScrollableResults results = s.createQuery( QUERY ).scroll();
			List list = new ArrayList();
			while ( results.next() ) {
				list.add( results.get( 0 ) );
			}
			assertResultFromAllUsers( list );
			s.close();
		}
		finally {
			deleteAll();
		}
	}

	public void testScrollOrderParentAsc() {
		try {
			insertTestData();
			Session s = openSession();
			ScrollableResults results = s.createQuery( QUERY + " order by p.name asc" ).scroll();
			List list = new ArrayList();
			while ( results.next() ) {
				list.add( results.get( 0 ) );
			}
			assertResultFromAllUsers( list );
			s.close();
		}
		finally {
			deleteAll();
		}
	}

	public void testScrollOrderParentDesc() {
		try {
			insertTestData();
			Session s = openSession();
			ScrollableResults results = s.createQuery( QUERY + " order by p.name desc" ).scroll();
			List list = new ArrayList();
			while ( results.next() ) {
				list.add( results.get( 0 ) );
			}
			assertResultFromAllUsers( list );
			s.close();
		}
		finally {
			deleteAll();
		}
	}

	public void testScrollOrderParentAscChildrenAsc() {
		try {
			insertTestData();
			Session s = openSession();
			ScrollableResults results = s.createQuery( QUERY + " order by p.name asc, c.name asc" ).scroll();
			List list = new ArrayList();
			while ( results.next() ) {
				list.add( results.get( 0 ) );
			}
			assertResultFromAllUsers( list );
			s.close();
		}
		finally {
			deleteAll();
		}
	}

	public void testScrollOrderParentAscChildrenDesc() {
		try {
			insertTestData();
			Session s = openSession();
			ScrollableResults results = s.createQuery( QUERY + " order by p.name asc, c.name desc" ).scroll();
			List list = new ArrayList();
			while ( results.next() ) {
				list.add( results.get( 0 ) );
			}
			assertResultFromAllUsers( list );
			s.close();
		}
		finally {
			deleteAll();
		}
	}

	private void assertResultFromAllUsers(List list) {
		assertEquals( "list is not correct size: ", 2, list.size() );
		for ( Iterator i = list.iterator(); i.hasNext(); ) {
			Parent parent = (Parent) i.next();
			assertEquals(
					"parent " + parent + " has incorrect collection(" + parent.getChildren() + ").",
					3,
					parent.getChildren().size()
			);
		}
	}

	private void deleteAll() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		List list = s.createQuery( "from Parent" ).list();
		for ( Iterator i = list.iterator(); i.hasNext(); ) {
			s.delete( (Parent) i.next() );
		}
		t.commit();
		s.close();
	}

	private void insertTestData() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.save( makeParent( "parent1", "child1-1", "child1-2", "child1-3" ) );
		s.save( makeParent( "parent2", "child2-1", "child2-2", "child2-3" ) );
		t.commit();
		s.close();
	}

	public String[] getMappings() {
		return new String[] { "hqlfetchscroll/ParentChild.hbm.xml" };
	}

	protected Object makeParent(String name, String child1, String child2, String child3) {
		Parent parent = new Parent( name );
		parent.addChild( new Child( child1 ) );
		parent.addChild( new Child( child2 ) );
		parent.addChild( new Child( child3 ) );
		return parent;
	}
}
