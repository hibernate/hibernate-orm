/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.jpa.test.graphs.queryhint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

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
