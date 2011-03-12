// $Id: JoinedSubclassFilterTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.subclassfilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Implementation of JoinedSubclassFilterTest.
 *
 * @author Steve Ebersole
 */
public class JoinedSubclassFilterTest extends FunctionalTestCase {

	public JoinedSubclassFilterTest(String name) {
		super( name );
	}

	public final String[] getMappings() {
		return new String[] { "subclassfilter/joined-subclass.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( JoinedSubclassFilterTest.class );
	}

	public void testFiltersWithJoinedSubclass() {
		Session s = openSession();
		s.enableFilter( "region" ).setParameter( "userRegion", "US" );
		Transaction t = s.beginTransaction();

		prepareTestData( s );
		s.clear();

		List results = s.createQuery( "from Person" ).list();
		assertEquals( "Incorrect qry result count", 4, results.size() );
		s.clear();

		results = s.createQuery( "from Employee" ).list();
		assertEquals( "Incorrect qry result count", 2, results.size() );
		Iterator itr = results.iterator();
		while ( itr.hasNext() ) {
			// find john
			final Person p = ( Person ) itr.next();
			if ( p.getName().equals( "John Doe" ) ) {
				Employee john = ( Employee ) p;
				assertEquals( "Incorrect fecthed minions count", 2, john.getMinions().size() );
				break;
			}
		}
		s.clear();

		// TODO : currently impossible to define a collection-level filter w/ joined-subclass elements that will filter based on a superclass column and function correctly in (theta only?) outer joins;
		// this is consistent with the behaviour of a collection-level where.
		// this might be one argument for "pulling" the attached class-level filters into collection assocations,
		// although we'd need some way to apply the appropriate alias in that scenario.
		results = new ArrayList( new HashSet( s.createQuery( "from Person as p left join fetch p.minions" ).list() ) );
		assertEquals( "Incorrect qry result count", 4, results.size() );
		itr = results.iterator();
		while ( itr.hasNext() ) {
			// find john
			final Person p = ( Person ) itr.next();
			if ( p.getName().equals( "John Doe" ) ) {
				Employee john = ( Employee ) p;
				assertEquals( "Incorrect fecthed minions count", 2, john.getMinions().size() );
				break;
			}
		}
		s.clear();

		results = new ArrayList( new HashSet( s.createQuery( "from Employee as p left join fetch p.minions" ).list() ) );
		assertEquals( "Incorrect qry result count", 2, results.size() );
		itr = results.iterator();
		while ( itr.hasNext() ) {
			// find john
			final Person p = ( Person ) itr.next();
			if ( p.getName().equals( "John Doe" ) ) {
				Employee john = ( Employee ) p;
				assertEquals( "Incorrect fecthed minions count", 2, john.getMinions().size() );
				break;
			}
		}

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery( "delete Customer where contactOwner is not null" ).executeUpdate();
		s.createQuery( "delete Employee where manager is not null" ).executeUpdate();
		s.createQuery( "delete Person" ).executeUpdate();
		t.commit();
		s.close();
	}

	private void prepareTestData(Session s) {
		Employee john = new Employee("John Doe");
		john.setCompany( "JBoss" );
		john.setDepartment( "hr" );
		john.setTitle( "hr guru" );
		john.setRegion( "US" );

		Employee polli = new Employee("Polli Wog");
		polli.setCompany( "JBoss" );
		polli.setDepartment( "hr" );
		polli.setTitle( "hr novice" );
		polli.setRegion( "US" );
		polli.setManager( john );
		john.getMinions().add( polli );

		Employee suzie = new Employee( "Suzie Q" );
		suzie.setCompany( "JBoss" );
		suzie.setDepartment( "hr" );
		suzie.setTitle( "hr novice" );
		suzie.setRegion( "EMEA" );
		suzie.setManager( john );
		john.getMinions().add( suzie );

		Customer cust = new Customer( "John Q Public" );
		cust.setCompany( "Acme" );
		cust.setRegion( "US" );
		cust.setContactOwner( john );

		Person ups = new Person( "UPS guy" );
		ups.setCompany( "UPS" );
		ups.setRegion( "US" );

		s.save( john );
		s.save( cust );
		s.save( ups );

		s.flush();
	}

}
