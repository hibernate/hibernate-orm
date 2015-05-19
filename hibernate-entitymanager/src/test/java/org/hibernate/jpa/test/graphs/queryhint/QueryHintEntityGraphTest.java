/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs.queryhint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Subgraph;

import org.hibernate.Hibernate;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.graphs.Company;
import org.hibernate.jpa.test.graphs.Employee;
import org.hibernate.jpa.test.graphs.Location;
import org.hibernate.jpa.test.graphs.Manager;
import org.hibernate.jpa.test.graphs.Market;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
public class QueryHintEntityGraphTest extends BaseEntityManagerFunctionalTestCase {
	
	// TODO: Currently, "loadgraph" and "fetchgraph" operate identically in JPQL.  The spec states that "fetchgraph"
	// shall use LAZY for non-specified attributes, ignoring their metadata.  Changes to ToOne select vs. join,
	// allowing queries to force laziness, etc. will require changes here and impl logic.
	
	@Test
	public void testLoadGraph() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		
		EntityGraph<Company> entityGraph = entityManager.createEntityGraph( Company.class );
		entityGraph.addAttributeNodes( "location" );
		entityGraph.addAttributeNodes( "markets" );
		Query query = entityManager.createQuery( "from " + Company.class.getName() );
		query.setHint( QueryHints.HINT_LOADGRAPH, entityGraph );
		Company company = (Company) query.getSingleResult();

		entityManager.getTransaction().commit();
		entityManager.close();
		
		assertFalse( Hibernate.isInitialized( company.employees ) );
		assertTrue( Hibernate.isInitialized( company.location ) );
		assertTrue( Hibernate.isInitialized( company.markets ) );
		// With "loadgraph", non-specified attributes use the fetch modes defined in the mappings.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should cause the follow-on selects to happen.
		assertTrue( Hibernate.isInitialized( company.phoneNumbers ) );
		
		entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		
		Subgraph<Employee> subgraph = entityGraph.addSubgraph( "employees" );
		subgraph.addAttributeNodes( "managers" );
		subgraph.addAttributeNodes( "friends" );
		Subgraph<Manager> subSubgraph = subgraph.addSubgraph( "managers", Manager.class );
		subSubgraph.addAttributeNodes( "managers" );
		subSubgraph.addAttributeNodes( "friends" );
		
		query = entityManager.createQuery( "from " + Company.class.getName() );
		query.setHint( QueryHints.HINT_LOADGRAPH, entityGraph );
		company = (Company) query.getSingleResult();
		
		entityManager.getTransaction().commit();
		entityManager.close();
		
		assertTrue( Hibernate.isInitialized( company.employees ) );
		assertTrue( Hibernate.isInitialized( company.location ) );
		assertEquals( 12345, company.location.zip );
		assertTrue( Hibernate.isInitialized( company.markets ) );
		// With "loadgraph", non-specified attributes use the fetch modes defined in the mappings.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should cause the follow-on selects to happen.
		assertTrue( Hibernate.isInitialized( company.phoneNumbers ) );
		
		boolean foundManager = false;
		Iterator<Employee> employeeItr = company.employees.iterator();
		while (employeeItr.hasNext()) {
			Employee employee = employeeItr.next();
			assertTrue( Hibernate.isInitialized( employee.managers ) );
			assertTrue( Hibernate.isInitialized( employee.friends ) );
			// test 1 more level
			Iterator<Manager> managerItr =  employee.managers.iterator();
			while (managerItr.hasNext()) {
				foundManager = true;
				Manager manager = managerItr.next();
				assertTrue( Hibernate.isInitialized( manager.managers ) );
				assertTrue( Hibernate.isInitialized( manager.friends ) );
			}
		}
		assertTrue(foundManager);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9457")
	public void testLoadGraphOrderByWithImplicitJoin() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		// create a new Company at a different location in a different zip code
		Location location = new Location();
		location.address = "123 somewhere";
		location.zip = 11234;
		entityManager.persist( location );
		Company companyNew = new Company();
		companyNew.location = location;
		entityManager.persist( companyNew );

		entityManager.getTransaction().commit();
		entityManager.close();

		entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		EntityGraph<Company> entityGraph = entityManager.createEntityGraph( Company.class );
		//entityGraph.addAttributeNodes( "location" );
		entityGraph.addAttributeNodes( "markets" );
		Query query = entityManager.createQuery( "from " + Company.class.getName() + " c order by c.location.zip, c.id" );
		query.setHint( QueryHints.HINT_LOADGRAPH, entityGraph );
		List results = query.getResultList();

		// we expect 3 results:
		// - 1st will be the Company with location.zip == 11234 with an empty markets collection
		// - 2nd and 3rd should be the Company with location.zip == 12345
		//   (2nd and 3rd are duplicated because that entity has 2 elements in markets collection
		assertEquals( 3, results.size() );

		Company companyResult = (Company) results.get( 0 );
		assertFalse( Hibernate.isInitialized( companyResult.employees ) );
		assertFalse( Hibernate.isInitialized( companyResult.location ) );
		// initialize and check zip
		// TODO: must have getters to access lazy entity after being initialized (why?)
		//assertEquals( 11234, companyResult.location.zip );
		assertEquals( 11234, companyResult.getLocation().getZip() );
		assertTrue( Hibernate.isInitialized( companyResult.markets ) );
		assertEquals( 0, companyResult.markets.size() );
		// With "loadgraph", non-specified attributes use the fetch modes defined in the mappings.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should cause the follow-on selects to happen.
		assertTrue( Hibernate.isInitialized( companyResult.phoneNumbers ) );
		assertEquals( 0, companyResult.phoneNumbers.size() );

		companyResult = (Company) results.get( 1 );
		assertFalse( Hibernate.isInitialized( companyResult.employees ) );
		assertFalse( Hibernate.isInitialized( companyResult.location ) );
		// initialize and check zip
		// TODO: must have getters to access lazy entity after being initialized (why?)
		//assertEquals( 12345, companyResult.location.zip );
		assertEquals( 12345, companyResult.getLocation().getZip() );
		assertTrue( Hibernate.isInitialized( companyResult.markets ) );
		assertEquals( 2, companyResult.markets.size() );
		// With "loadgraph", non-specified attributes use the fetch modes defined in the mappings.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should cause the follow-on selects to happen.
		assertTrue( Hibernate.isInitialized( companyResult.phoneNumbers ) );
		assertEquals( 2, companyResult.phoneNumbers.size() );

		assertSame( companyResult, results.get( 2 ) );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9448")
	public void testLoadGraphWithRestriction() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		EntityGraph<Company> entityGraph = entityManager.createEntityGraph( Company.class );
		entityGraph.addAttributeNodes( "location" );
		entityGraph.addAttributeNodes( "markets" );
		Query query = entityManager.createQuery( "from " + Company.class.getName() + " where location.zip = :zip")
				.setParameter( "zip", 12345 );
		query.setHint( QueryHints.HINT_LOADGRAPH, entityGraph );
		Company company = (Company) query.getSingleResult();

		entityManager.getTransaction().commit();
		entityManager.close();

		assertFalse( Hibernate.isInitialized( company.employees ) );
		assertTrue( Hibernate.isInitialized( company.location ) );
		assertTrue( Hibernate.isInitialized( company.markets ) );
		// With "loadgraph", non-specified attributes use the fetch modes defined in the mappings.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should cause the follow-on selects to happen.
		assertTrue( Hibernate.isInitialized( company.phoneNumbers ) );

		entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		Subgraph<Employee> subgraph = entityGraph.addSubgraph( "employees" );
		subgraph.addAttributeNodes( "managers" );
		subgraph.addAttributeNodes( "friends" );
		Subgraph<Manager> subSubgraph = subgraph.addSubgraph( "managers", Manager.class );
		subSubgraph.addAttributeNodes( "managers" );
		subSubgraph.addAttributeNodes( "friends" );

		query = entityManager.createQuery( "from " + Company.class.getName()  + " where location.zip = :zip" )
				.setParameter( "zip", 12345 );
		query.setHint( QueryHints.HINT_LOADGRAPH, entityGraph );
		company = (Company) query.getSingleResult();

		entityManager.getTransaction().commit();
		entityManager.close();

		assertTrue( Hibernate.isInitialized( company.employees ) );
		assertTrue( Hibernate.isInitialized( company.location ) );
		assertTrue( Hibernate.isInitialized( company.markets ) );
		// With "loadgraph", non-specified attributes use the fetch modes defined in the mappings.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should cause the follow-on selects to happen.
		assertTrue( Hibernate.isInitialized( company.phoneNumbers ) );

		boolean foundManager = false;
		Iterator<Employee> employeeItr = company.employees.iterator();
		while (employeeItr.hasNext()) {
			Employee employee = employeeItr.next();
			assertTrue( Hibernate.isInitialized( employee.managers ) );
			assertTrue( Hibernate.isInitialized( employee.friends ) );
			// test 1 more level
			Iterator<Manager> managerItr =  employee.managers.iterator();
			while (managerItr.hasNext()) {
				foundManager = true;
				Manager manager = managerItr.next();
				assertTrue( Hibernate.isInitialized( manager.managers ) );
				assertTrue( Hibernate.isInitialized( manager.friends ) );
			}
		}
		assertTrue(foundManager);
	}

	@Test
	public void testEntityGraphWithExplicitFetch() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		
		EntityGraph<Company> entityGraph = entityManager.createEntityGraph( Company.class );
		entityGraph.addAttributeNodes( "location" );
		entityGraph.addAttributeNodes( "markets" );
		entityGraph.addAttributeNodes( "employees" );
		// Ensure the EntityGraph and explicit fetches do not conflict.
		Query query = entityManager.createQuery( "from " + Company.class.getName()
				+ " as c left join fetch c.location left join fetch c.employees" );
		query.setHint( QueryHints.HINT_LOADGRAPH, entityGraph );
		Company company = (Company) query.getSingleResult();
		
		entityManager.getTransaction().commit();
		entityManager.close();
		
		assertTrue( Hibernate.isInitialized( company.employees ) );
		assertTrue( Hibernate.isInitialized( company.location ) );
		assertTrue( Hibernate.isInitialized( company.markets ) );
		// With "loadgraph", non-specified attributes use the fetch modes defined in the mappings.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should cause the follow-on selects to happen.
		assertTrue( Hibernate.isInitialized( company.phoneNumbers ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9448")
	public void testEntityGraphWithExplicitFetchAndRestriction() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		EntityGraph<Company> entityGraph = entityManager.createEntityGraph( Company.class );
		entityGraph.addAttributeNodes( "location" );
		entityGraph.addAttributeNodes( "markets" );
		entityGraph.addAttributeNodes( "employees" );
		// Ensure the EntityGraph and explicit fetches do not conflict.
		Query query = entityManager.createQuery( "from " + Company.class.getName()
						+ " as c left join fetch c.location left join fetch c.employees where c.location.zip = :zip")
				.setParameter( "zip", 12345 );
		query.setHint( QueryHints.HINT_LOADGRAPH, entityGraph );
		Company company = (Company) query.getSingleResult();

		entityManager.getTransaction().commit();
		entityManager.close();

		assertTrue( Hibernate.isInitialized( company.employees ) );
		assertTrue( Hibernate.isInitialized( company.location ) );
		assertTrue( Hibernate.isInitialized( company.markets ) );
		// With "loadgraph", non-specified attributes use the fetch modes defined in the mappings.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should cause the follow-on selects to happen.
		assertTrue( Hibernate.isInitialized( company.phoneNumbers ) );
	}

	@Before
	public void createData() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		
		Manager manager1 = new Manager();
		entityManager.persist( manager1 );
		
		Manager manager2 = new Manager();
		manager2.managers.add( manager1 );
		entityManager.persist( manager2 );
		
		Employee employee = new Employee();
		employee.managers.add( manager1 );
		entityManager.persist( employee );
		
		Location location = new Location();
		location.address = "123 somewhere";
		location.zip = 12345;
		entityManager.persist( location );
		
		Company company = new Company();
		company.employees.add( employee );
		company.employees.add( manager1 );
		company.employees.add( manager2 );
		company.location = location;
		company.markets.add( Market.SERVICES );
		company.markets.add( Market.TECHNOLOGY );
		company.phoneNumbers.add( "012-345-6789" );
		company.phoneNumbers.add( "987-654-3210" );
		entityManager.persist( company );
		
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Company.class, Employee.class, Manager.class, Location.class };
	}
}
