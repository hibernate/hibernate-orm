/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs.queryhint;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.orm.test.jpa.graphs.Company;
import org.hibernate.orm.test.jpa.graphs.CompanyFetchProfile;
import org.hibernate.orm.test.jpa.graphs.Course;
import org.hibernate.orm.test.jpa.graphs.Employee;
import org.hibernate.orm.test.jpa.graphs.Location;
import org.hibernate.orm.test.jpa.graphs.Manager;
import org.hibernate.orm.test.jpa.graphs.Market;
import org.hibernate.orm.test.jpa.graphs.Student;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Subgraph;
import jakarta.persistence.TypedQuery;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_FETCH_GRAPH;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_FETCH_GRAPH;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Brett Meyer
 * @author Nathan Xu
 */
public class QueryHintEntityGraphTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testLoadGraph() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		EntityGraph<Company> entityGraph = entityManager.createEntityGraph( Company.class );
		entityGraph.addAttributeNodes( "location" );
		entityGraph.addAttributeNodes( "markets" );
		Query query = entityManager.createQuery( "from " + Company.class.getName() );
		query.setHint( HINT_SPEC_LOAD_GRAPH, entityGraph );
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
		query.setHint( HINT_SPEC_LOAD_GRAPH, entityGraph );
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
	@JiraKey(value = "https://hibernate.atlassian.net/browse/HHH-14855")
	public void testEntityGraphStringQueryHint() {
		TransactionUtil2.inTransaction( entityManagerFactory(), (session) -> {
			final String hql = "from Company c";
			final String graphString = "Company( location, markets )";

			{
				TypedQuery<Company> query = session.createQuery( hql, Company.class );
				final Company company = query.getSingleResult();
				assertFalse( Hibernate.isInitialized( company.employees ) );
				assertFalse( Hibernate.isInitialized( company.location ) );
				assertFalse( Hibernate.isInitialized( company.markets ) );
			}

			{
				TypedQuery<Company> query = session.createQuery( hql, Company.class );
				query.setHint( HINT_SPEC_FETCH_GRAPH, graphString );
				final Company company = query.getSingleResult();
				assertFalse( Hibernate.isInitialized( company.employees ) );
				assertTrue( Hibernate.isInitialized( company.location ) );
				assertTrue( Hibernate.isInitialized( company.markets ) );
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-8776")
	public void testFetchGraph() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		EntityGraph<Company> entityGraph = entityManager.createEntityGraph( Company.class );
		entityGraph.addAttributeNodes( "location" );
		entityGraph.addAttributeNodes( "markets" );
		Query query = entityManager.createQuery( "from " + Company.class.getName() );
		query.setHint( HINT_JAVAEE_FETCH_GRAPH, entityGraph );
		Company company = (Company) query.getSingleResult();

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

		query = entityManager.createQuery( "from " + Company.class.getName() );
		query.setHint( HINT_JAVAEE_FETCH_GRAPH, entityGraph );
		company = (Company) query.getSingleResult();

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
	@JiraKey(value = "HHH-8776")
	public void testFetchGraphTakingPrecedenceOverFetchProfile() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		entityManager.unwrap( Session.class ).enableFetchProfile( "company.location" );

		EntityGraph<CompanyFetchProfile> entityGraph = entityManager.createEntityGraph( CompanyFetchProfile.class );
		entityGraph.addAttributeNodes( "markets" );
		Query query = entityManager.createQuery( "from " + CompanyFetchProfile.class.getName() );
		query.setHint( HINT_JAVAEE_FETCH_GRAPH, entityGraph );
		CompanyFetchProfile company = (CompanyFetchProfile) query.getSingleResult();

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

		query = entityManager.createQuery( "from " + CompanyFetchProfile.class.getName() );
		query.setHint( HINT_JAVAEE_FETCH_GRAPH, entityGraph );
		company = (CompanyFetchProfile) query.getSingleResult();

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

	@Test
	@JiraKey( value = "HHH-9457")
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
		query.setHint( HINT_SPEC_LOAD_GRAPH, entityGraph );
		List results = query.getResultList();

		// - 1st will be the Company with location.zip == 11234 with an empty markets collection
		// - 2nd should be the Company with location.zip == 12345
		assertEquals( 2, results.size() );

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

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	@JiraKey( value = "HHH-9448")
	public void testLoadGraphWithRestriction() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		EntityGraph<Company> entityGraph = entityManager.createEntityGraph( Company.class );
		entityGraph.addAttributeNodes( "location" );
		entityGraph.addAttributeNodes( "markets" );
		Query query = entityManager.createQuery( "from " + Company.class.getName() + " where location.zip = :zip")
				.setParameter( "zip", 12345 );
		query.setHint( HINT_SPEC_LOAD_GRAPH, entityGraph );
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
		query.setHint( HINT_SPEC_LOAD_GRAPH, entityGraph );
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
		query.setHint( HINT_SPEC_LOAD_GRAPH, entityGraph );
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
	@JiraKey( value = "HHH-9448")
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
				.setParameter("zip", 12345);
		query.setHint( HINT_SPEC_LOAD_GRAPH, entityGraph );
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
	@JiraKey(value = "HHH-9374")
	public void testEntityGraphWithCollectionSubquery(){
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		EntityGraph<Company> entityGraph = entityManager.createEntityGraph(Company.class);
		entityGraph.addAttributeNodes("location");
		Query query = entityManager.createQuery("select c from " + Company.class.getName() + " c where c.employees IS EMPTY");
		query.setHint(HINT_SPEC_LOAD_GRAPH, entityGraph);
		query.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	@JiraKey(value = "HHH-11569")
	public void testCollectionSizeLoadedWithGraph() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			Student student1 = new Student();
			student1.setId( 1 );
			student1.setName( "Student 1" );
			Student student2 = new Student();
			student2.setId( 2 );
			student2.setName( "Student 2" );

			Course course1 = new Course();
			course1.setName( "Full Time" );
			Course course2 = new Course();
			course2.setName( "Part Time" );

			Set<Course> std1Courses = new HashSet<Course>();
			std1Courses.add( course1 );
			std1Courses.add( course2 );
			student1.setCourses( std1Courses );

			Set<Course> std2Courses = new HashSet<Course>();
			std2Courses.add( course2 );
			student2.setCourses( std2Courses );

			entityManager.persist( student1 );
			entityManager.persist( student2 );

		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			EntityGraph<?> graph = entityManager.getEntityGraph( "Student.Full" );

			List<Student> students = entityManager.createNamedQuery( "LIST_OF_STD", Student.class )
					.setHint( HINT_JAVAEE_FETCH_GRAPH, graph )
					.getResultList();

			assertEquals( 2, students.size() );
		});
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

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Company.class, CompanyFetchProfile.class, Employee.class, Manager.class, Location.class, Course.class, Student.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG );
	}
}
