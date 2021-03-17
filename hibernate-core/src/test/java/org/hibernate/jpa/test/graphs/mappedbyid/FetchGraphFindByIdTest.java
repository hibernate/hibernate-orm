package org.hibernate.jpa.test.graphs.mappedbyid;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.graphs.*;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Nathan Xu
 */
public class FetchGraphFindByIdTest extends BaseEntityManagerFunctionalTestCase {

	private long companyId;
	
	private long companyWithFetchProfileId;
	
	@Test
	@TestForIssue(jiraKey = "HHH-8776")
	public void testFetchGraphByFind() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		EntityGraph<Company> entityGraph = entityManager.createEntityGraph( Company.class );
		entityGraph.addAttributeNodes( "location" );
		entityGraph.addAttributeNodes( "markets" );

		Map<String, Object> properties = Collections.singletonMap( "javax.persistence.fetchgraph", entityGraph );

		Company company = entityManager.find( Company.class, companyId, properties );

		entityManager.getTransaction().commit();
		entityManager.close();

		assertFalse( Hibernate.isInitialized( company.employees ) );
		assertTrue( Hibernate.isInitialized( company.location ) );
		assertTrue( Hibernate.isInitialized( company.markets ) );
		// With "fetchgraph", non-specified attributes effect 'lazy' mode.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should not be initialized.
		assertFalse( Hibernate.isInitialized( company.phoneNumbers ) );

		entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		Subgraph<Employee> subgraph = entityGraph.addSubgraph( "employees" );
		subgraph.addAttributeNodes( "managers" );
		subgraph.addAttributeNodes( "friends" );
		Subgraph<Manager> subSubgraph = subgraph.addSubgraph( "managers", Manager.class );
		subSubgraph.addAttributeNodes( "managers" );
		subSubgraph.addAttributeNodes( "friends" );

		company = entityManager.find( Company.class, companyId, properties );

		entityManager.getTransaction().commit();
		entityManager.close();

		assertTrue( Hibernate.isInitialized( company.employees ) );
		assertTrue( Hibernate.isInitialized( company.location ) );
		assertEquals( 12345, company.location.zip );
		assertTrue( Hibernate.isInitialized( company.markets ) );
		// With "fetchgraph", non-specified attributes effect 'lazy' mode.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should not be initialized.
		assertFalse( Hibernate.isInitialized( company.phoneNumbers ) );

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
	@TestForIssue(jiraKey = "HHH-8776")
	public void testFetchGraphByFindTakingPrecedenceOverFetchProfile() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		entityManager.unwrap( Session.class ).enableFetchProfile("company.location");
		
		EntityGraph<CompanyFetchProfile> entityGraph = entityManager.createEntityGraph( CompanyFetchProfile.class );
		entityGraph.addAttributeNodes( "markets" );

		Map<String, Object> properties = Collections.singletonMap( "javax.persistence.fetchgraph", entityGraph );

		CompanyFetchProfile company = entityManager.find( CompanyFetchProfile.class, companyWithFetchProfileId, properties );

		entityManager.getTransaction().commit();
		entityManager.close();

		assertFalse( Hibernate.isInitialized( company.employees ) );
		assertFalse( Hibernate.isInitialized( company.location ) ); // should be initialized if 'company.location' fetch profile takes effect
		assertTrue( Hibernate.isInitialized( company.markets ) );
		// With "fetchgraph", non-specified attributes effect 'lazy' mode.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should not be initialized.
		assertFalse( Hibernate.isInitialized( company.phoneNumbers ) );

		entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		Subgraph<Employee> subgraph = entityGraph.addSubgraph( "employees" );
		subgraph.addAttributeNodes( "managers" );
		subgraph.addAttributeNodes( "friends" );
		Subgraph<Manager> subSubgraph = subgraph.addSubgraph( "managers", Manager.class );
		subSubgraph.addAttributeNodes( "managers" );
		subSubgraph.addAttributeNodes( "friends" );

		company = entityManager.find( CompanyFetchProfile.class, companyWithFetchProfileId, properties );

		entityManager.getTransaction().commit();
		entityManager.close();

		assertTrue( Hibernate.isInitialized( company.employees ) );
		assertFalse( Hibernate.isInitialized( company.location ) ); // should be initialized if 'company.location' fetch profile takes effect
		assertTrue( Hibernate.isInitialized( company.markets ) );
		// With "fetchgraph", non-specified attributes effect 'lazy' mode.  So, here,
		// @ElementCollection(fetch = FetchType.EAGER) should not be initialized.
		assertFalse( Hibernate.isInitialized( company.phoneNumbers ) );

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
		companyId = company.id;

		CompanyFetchProfile companyFetchProfile = new CompanyFetchProfile();
		companyFetchProfile.employees.add( employee );
		companyFetchProfile.employees.add( manager1 );
		companyFetchProfile.employees.add( manager2 );
		companyFetchProfile.location = location;
		companyFetchProfile.markets.add( Market.SERVICES );
		companyFetchProfile.markets.add( Market.TECHNOLOGY );
		companyFetchProfile.phoneNumbers.add( "012-345-6789" );
		companyFetchProfile.phoneNumbers.add( "987-654-3210" );
		entityManager.persist( companyFetchProfile );
		companyWithFetchProfileId = companyFetchProfile.id;
		
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Company.class, CompanyFetchProfile.class, Employee.class, Manager.class, Location.class, Course.class, Student.class };
	}
	
}
