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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Subgraph;

import org.hibernate.Hibernate;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
public class QueryHintEntityGraphTest extends BaseEntityManagerFunctionalTestCase {
	
	@Test
	public void testQueryHintEntityGraph() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		
		EntityGraph<Company> entityGraph = entityManager.createEntityGraph( Company.class );
		entityGraph.addAttributeNodes( "location" );
		entityGraph.addAttributeNodes( "markets" );
		Query query = entityManager.createQuery( "from " + Company.class.getName() );
		query.setHint( QueryHints.HINT_FETCHGRAPH, entityGraph );
		Company company = (Company) query.getSingleResult();
		
		entityManager.getTransaction().commit();
		entityManager.close();
		
		assertFalse( Hibernate.isInitialized( company.employees ) );
		assertTrue( Hibernate.isInitialized( company.location ) );
		assertTrue( Hibernate.isInitialized( company.markets ) );
		
		entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		
		Subgraph<Employee> subgraph1 = entityGraph.addSubgraph( "employees" );
		subgraph1.addAttributeNodes( "managers" );
		Subgraph<Employee> subgraph2 = subgraph1.addSubgraph( "managers" );
		subgraph2.addAttributeNodes( "managers" );
		query = entityManager.createQuery( "from " + Company.class.getName() );
		query.setHint( QueryHints.HINT_FETCHGRAPH, entityGraph );
		company = (Company) query.getSingleResult();
		
		entityManager.getTransaction().commit();
		entityManager.close();
		
		assertTrue( Hibernate.isInitialized( company.employees ) );
		assertTrue( Hibernate.isInitialized( company.location ) );
		assertTrue( Hibernate.isInitialized( company.markets ) );
		
		boolean foundManager = false;
		Iterator<Employee> employeeItr = company.employees.iterator();
		while (employeeItr.hasNext()) {
			Employee employee = employeeItr.next();
			assertTrue( Hibernate.isInitialized( employee.managers ) );
			Iterator<Employee> managerItr =  employee.managers.iterator();
			while (managerItr.hasNext()) {
				foundManager = true;
				assertTrue( Hibernate.isInitialized( managerItr.next().managers ) );
			}
		}
		assertTrue(foundManager);
	}
	
	@Test
	public void testQueryHintEntityGraphWithExplicitFetch() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		
		EntityGraph<Company> entityGraph = entityManager.createEntityGraph( Company.class );
		entityGraph.addAttributeNodes( "location" );
		entityGraph.addAttributeNodes( "markets" );
		entityGraph.addAttributeNodes( "employees" );
		Query query = entityManager.createQuery( "from " + Company.class.getName()
				+ " as c left join fetch c.location left join fetch c.employees" );
		query.setHint( QueryHints.HINT_FETCHGRAPH, entityGraph );
		Company company = (Company) query.getSingleResult();
		
		entityManager.getTransaction().commit();
		entityManager.close();
		
		assertTrue( Hibernate.isInitialized( company.employees ) );
		assertTrue( Hibernate.isInitialized( company.location ) );
		assertTrue( Hibernate.isInitialized( company.markets ) );
	}
	
	@Before
	public void createData() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		
		Employee manager1 = new Employee();
		entityManager.persist( manager1 );
		
		Employee manager2 = new Employee();
		manager2.managers = new HashSet<Employee>();
		manager2.managers.add( manager1 );
		entityManager.persist( manager2 );
		
		Employee employee = new Employee();
		employee.managers = new HashSet<Employee>();
		employee.managers.add( manager1 );
		entityManager.persist( employee );
		
		Location location = new Location();
		location.address = "123 somewhere";
		location.zip = 12345;
		entityManager.persist( location );
		
		Company company = new Company();
		company.employees = new HashSet<Employee>();
		company.employees.add( employee );
		company.employees.add( manager1 );
		company.employees.add( manager2 );
		company.location = location;
		company.markets = new HashSet<Market>();
		company.markets.add( Market.SERVICES );
		company.markets.add( Market.TECHNOLOGY );
		entityManager.persist( company );
		
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Company.class, Employee.class, Location.class };
	}
	
	@Entity
	private static class Company implements Serializable {
		@Id @GeneratedValue
		public long id;
		
		@OneToMany
		public Set<Employee> employees;
		
		@OneToOne(fetch = FetchType.LAZY)
		public Location location;
		
		@ElementCollection
		public Set<Market> markets;
	}
	
	@Entity
	private static class Employee {
		@Id @GeneratedValue
		public long id;
		
		@ManyToMany
		public Set<Employee> managers;
	}
	
	@Entity
	private static class Location {
		public Location() { }
		
		@Id @GeneratedValue
		public long id;
		
		public String address;
		
		public int zip;
	}
	
	private static enum Market {
		SERVICES, TECHNOLOGY, INDUSTRIAL;
	}
}
