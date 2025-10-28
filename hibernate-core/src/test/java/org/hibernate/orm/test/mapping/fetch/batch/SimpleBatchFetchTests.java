/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.batch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test for batch fetching.
 *
 * Partially acts as a baseline for SubselectFetchCollectionFromBatchTest
 */
@DomainModel( annotatedClasses = { SimpleBatchFetchTests.EmployeeGroup.class, SimpleBatchFetchTests.Employee.class } )
@SessionFactory( useCollectingStatementInspector = true )
public class SimpleBatchFetchTests {

	@Test
	public void baselineTest(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final EmployeeGroup group1 = session.getReference( EmployeeGroup.class, 1 );
			final EmployeeGroup group2 = session.getReference( EmployeeGroup.class, 2 );

			assertThat( Hibernate.isInitialized( group1 ) ).isFalse();
			assertThat( Hibernate.isInitialized( group2 ) ).isFalse();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 0 );

			Hibernate.initialize( group1 );

			assertThat( Hibernate.isInitialized( group1 ) ).isTrue();
			assertThat( Hibernate.isInitialized( group2 ) ).isTrue();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( group1.getEmployees() ) ).isFalse();
			assertThat( Hibernate.isInitialized( group2.getEmployees() ) ).isFalse();

			Hibernate.initialize( group1.getEmployees() );

			assertThat( Hibernate.isInitialized( group1 ) ).isTrue();
			assertThat( Hibernate.isInitialized( group2 ) ).isTrue();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
			assertThat( Hibernate.isInitialized( group1.getEmployees() ) ).isTrue();
			assertThat( Hibernate.isInitialized( group2.getEmployees() ) ).isTrue();

			assertThat( ( (PersistentCollection) group1.getEmployees() ).getOwner() ).isNotInstanceOf( HibernateProxy.class );
		} );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EmployeeGroup group1 = new EmployeeGroup(1, "QA");
			final Employee employee1 = new Employee(100, "Jane");
			final Employee employee2 = new Employee(101, "Jeff");
			group1.addEmployee(employee1);
			group1.addEmployee(employee2);

			EmployeeGroup group2 = new EmployeeGroup(2, "R&D");
			Employee employee3 = new Employee(200, "Joan");
			Employee employee4 = new Employee(201, "John");
			group2.addEmployee(employee3);
			group2.addEmployee( employee4 );

			session.persist( group1 );
			session.persist( group2 );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name="EmployeeGroup")
	@Table(name = "EmployeeGroup")
	@BatchSize( size = 100 )
	public static class EmployeeGroup {
		@Id
		private Integer id;
		private String name;

		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		private Employee lead;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinTable(name="EmployeeGroup_employees")
		@BatchSize( size = 100 )
		private List<Employee> employees = new ArrayList<>();

		@SuppressWarnings("unused")
		protected EmployeeGroup() {
		}

		public EmployeeGroup(int id,String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;

		}

		public void setName(String name) {
			this.name = name;
		}

		public Employee getLead() {
			return lead;
		}

		public void setLead(Employee lead) {
			this.lead = lead;
		}

		public boolean addEmployee(Employee employee) {
			return employees.add(employee);
		}

		public List<Employee> getEmployees() {
			return employees;
		}

		@Override
		public String toString() {
			return "EmployeeGroup(" + id.toString() + " : " + name + ")";
		}
	}


	@Entity( name="Employee")
	@Table(name = "Employee")
	public static class Employee {
		@Id
		private Integer id;
		private String name;

		@SuppressWarnings("unused")
		private Employee() {
		}

		public Employee(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "Employee(" + id.toString() + " : " + name + ")";
		}
	}
}
