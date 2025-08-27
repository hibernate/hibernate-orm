/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.subselect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Stephen Fikes
 * @author Gail Badner
 */
@ServiceRegistry( settings = @Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ) )
@DomainModel( annotatedClasses = { SubselectFetchCollectionFromBatchTest.EmployeeGroup.class, SubselectFetchCollectionFromBatchTest.Employee.class } )
@SessionFactory( useCollectingStatementInspector = true )
public class SubselectFetchCollectionFromBatchTest {

	@Test
	@JiraKey( value = "HHH-10679")
	public void testSubselectFetchFromEntityBatch(SessionFactoryScope scope) {
		final EmployeeGroup[] createdGroups = scope.fromTransaction( (s) -> {
			EmployeeGroup group1 = new EmployeeGroup();
			Employee employee1 = new Employee("Jane");
			Employee employee2 = new Employee("Jeff");
			group1.addEmployee(employee1);
			group1.addEmployee(employee2);
			EmployeeGroup group2 = new EmployeeGroup();
			Employee employee3 = new Employee("Joan");
			Employee employee4 = new Employee("John");
			group2.addEmployee(employee3);
			group2.addEmployee( employee4 );

			s.persist( group1 );
			s.persist( group2 );

			return new EmployeeGroup[] { group1, group2 };
		});

		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (s) -> {
			EmployeeGroup[] loadedGroups = new EmployeeGroup[] {
					s.getReference(EmployeeGroup.class, createdGroups[0].getId()),
					s.getReference(EmployeeGroup.class, createdGroups[1].getId())
			};

			// there should have been no SQL queries performed and loadedGroups should only contain proxies
			assertThat( statementInspector.getSqlQueries() ).hasSize( 0 );
			for (EmployeeGroup group : loadedGroups) {
				assertFalse( Hibernate.isInitialized( group ) );
			}

			// because EmployeeGroup defines batch fetching, both of the EmployeeGroup references
			// should get initialized when we initialize one of them
			Hibernate.initialize( loadedGroups[0] );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( loadedGroups[0] ) ).isTrue();
			assertThat( Hibernate.isInitialized( loadedGroups[1] ) ).isTrue();

			// their collections however should still be unintialized
			assertThat( Hibernate.isInitialized( loadedGroups[0].getEmployees() ) ).isFalse();
			assertThat( Hibernate.isInitialized( loadedGroups[1].getEmployees() ) ).isFalse();

			statementInspector.clear();

			// now initialize the collection in the first; collections in both loadedGroups
			// should get initialized
			Hibernate.initialize( loadedGroups[0].getEmployees() );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );

			// both collections should be initialized
			assertThat( Hibernate.isInitialized( loadedGroups[0].getEmployees() ) ).isTrue();
			assertThat( Hibernate.isInitialized( loadedGroups[1].getEmployees() ) ).isTrue();
		} );
	}

	@Test
	public void testSubselectFetchFromQueryList(SessionFactoryScope scope) {
		final Long[] createdIds = scope.fromTransaction( (s) -> {
			EmployeeGroup group1 = new EmployeeGroup();
			Employee employee1 = new Employee("Jane");
			Employee employee2 = new Employee("Jeff");
			group1.addEmployee(employee1);
			group1.addEmployee(employee2);
			EmployeeGroup group2 = new EmployeeGroup();
			Employee employee3 = new Employee("Joan");
			Employee employee4 = new Employee("John");
			group2.addEmployee(employee3);
			group2.addEmployee( employee4 );

			s.persist( group1 );
			s.persist( group2 );

			return new Long[] { group1.id, group2.id };
		} );

		scope.getSessionFactory().getStatistics().clear();

		scope.inTransaction( (s) -> {
			List<EmployeeGroup> results = s
					.createQuery( "from EmployeeGroup where id in :groups" )
					.setParameterList( "groups", createdIds )
					.list();

			assertEquals( 1, scope.getSessionFactory().getStatistics().getPrepareStatementCount() );
			scope.getSessionFactory().getStatistics().clear();

			for (EmployeeGroup group : results) {
				assertTrue( Hibernate.isInitialized( group ) );
				assertFalse( Hibernate.isInitialized( group.getEmployees() ) );
			}

			assertEquals( 0, scope.getSessionFactory().getStatistics().getPrepareStatementCount() );

			// now initialize the collection in the first; collections in both groups
			// should get initialized
			Hibernate.initialize( results.get( 0 ).getEmployees() );

			assertEquals( 1, scope.getSessionFactory().getStatistics().getPrepareStatementCount() );
			scope.getSessionFactory().getStatistics().clear();

			// all collections should be initialized now
			for (EmployeeGroup group : results) {
				assertTrue( Hibernate.isInitialized( group.getEmployees() ) );
			}

			assertEquals( 0, scope.getSessionFactory().getStatistics().getPrepareStatementCount() );
		} );

	}

	@Test
	@JiraKey( value = "HHH-10679")
	public void testMultiSubselectFetchSamePersisterQueryList(SessionFactoryScope scope) {
		final Long[] createdIds = scope.fromTransaction( (s) -> {
			EmployeeGroup group1 = new EmployeeGroup();
			Employee employee1 = new Employee("Jane");
			Employee employee2 = new Employee("Jeff");
			group1.addEmployee( employee1 );
			group1.addEmployee( employee2 );
			group1.setManager( new Employee( "group1 manager" ) );
			group1.getManager().addCollaborator( new Employee( "group1 manager's collaborator#1" ) );
			group1.getManager().addCollaborator( new Employee( "group1 manager's collaborator#2" ) );
			group1.setLead( new Employee( "group1 lead" ) );
			group1.getLead().addCollaborator( new Employee( "group1 lead's collaborator#1" ) );
			EmployeeGroup group2 = new EmployeeGroup();
			Employee employee3 = new Employee("Joan");
			Employee employee4 = new Employee("John");
			group2.addEmployee( employee3 );
			group2.addEmployee( employee4 );
			group2.setManager( new Employee( "group2 manager" ) );
			group2.getManager().addCollaborator( new Employee( "group2 manager's collaborator#1" ) );
			group2.getManager().addCollaborator( new Employee( "group2 manager's collaborator#2" ) );
			group2.getManager().addCollaborator( new Employee( "group2 manager's collaborator#3" ) );
			group2.setLead( new Employee( "group2 lead" ) );
			group2.getLead().addCollaborator( new Employee( "group2 lead's collaborator#1" ) );
			group2.getLead().addCollaborator( new Employee( "group2 lead's collaborator#2" ) );

			s.persist( group1 );
			s.persist( group2 );

			return new Long[] { group1.id, group2.id };
		} );

		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		sessionFactory.getStatistics().clear();

		scope.inTransaction( (s) -> {
			EmployeeGroup[] loadedGroups = new EmployeeGroup[] {
					s.getReference(EmployeeGroup.class, createdIds[0]),
					s.getReference(EmployeeGroup.class, createdIds[1])
			};

			// loadedGroups should only contain proxies
			assertEquals( 0, sessionFactory.getStatistics().getPrepareStatementCount() );

			for (EmployeeGroup group : loadedGroups) {
				assertFalse( Hibernate.isInitialized( group ) );
			}

			assertEquals( 0, sessionFactory.getStatistics().getPrepareStatementCount() );


			for ( EmployeeGroup group : loadedGroups ) {
				// Both loadedGroups get initialized  and are added to the PersistenceContext when i == 0;
				// Still need to call Hibernate.initialize( loadedGroups[i] ) for i > 0 so that the entity
				// in the PersistenceContext gets assigned to its respective proxy target (is this a
				// bug???)
				Hibernate.initialize( group );
				assertTrue( Hibernate.isInitialized( group ) );
				assertTrue( Hibernate.isInitialized( group.getLead() ) );
				assertFalse( Hibernate.isInitialized( group.getLead().getCollaborators() ) );
				assertTrue( Hibernate.isInitialized( group.getManager() ) );
				assertFalse( Hibernate.isInitialized( group.getManager().getCollaborators() ) );
				// the collections should be uninitialized
				assertFalse( Hibernate.isInitialized( group.getEmployees() ) );
			}

			// both Group proxies should have been loaded in the same batch;
			assertEquals( 1, sessionFactory.getStatistics().getPrepareStatementCount() );
			sessionFactory.getStatistics().clear();

			for ( EmployeeGroup group : loadedGroups ) {
				assertTrue( Hibernate.isInitialized( group ) );
				assertFalse( Hibernate.isInitialized( group.getEmployees() ) );
			}

			assertEquals( 0, sessionFactory.getStatistics().getPrepareStatementCount() );

			// now initialize the collection in the first; collections in both loadedGroups
			// should get initialized
			Hibernate.initialize( loadedGroups[0].getEmployees() );

			assertEquals( 1, sessionFactory.getStatistics().getPrepareStatementCount() );
			sessionFactory.getStatistics().clear();

			// all EmployeeGroup#employees should be initialized now
			for (EmployeeGroup group : loadedGroups) {
				assertTrue( Hibernate.isInitialized( group.getEmployees() ) );
				assertFalse( Hibernate.isInitialized( group.getLead().getCollaborators() ) );
				assertFalse( Hibernate.isInitialized( group.getManager().getCollaborators() ) );
			}

			assertEquals( 0, sessionFactory.getStatistics().getPrepareStatementCount() );

			// now initialize loadedGroups[0].getLead().getCollaborators();
			// loadedGroups[1].getLead().getCollaborators() should also be initialized
			Hibernate.initialize( loadedGroups[0].getLead().getCollaborators() );

			assertEquals( 1, sessionFactory.getStatistics().getPrepareStatementCount() );
			sessionFactory.getStatistics().clear();

			for (EmployeeGroup group : loadedGroups) {
				assertTrue( Hibernate.isInitialized( group.getLead().getCollaborators() ) );
				assertFalse( Hibernate.isInitialized( group.getManager().getCollaborators() ) );
			}

			assertEquals( 0, sessionFactory.getStatistics().getPrepareStatementCount() );

			// now initialize loadedGroups[0].getManager().getCollaborators();
			// loadedGroups[1].getManager().getCollaborators() should also be initialized
			Hibernate.initialize( loadedGroups[0].getManager().getCollaborators() );

			assertEquals( 1, sessionFactory.getStatistics().getPrepareStatementCount() );
			sessionFactory.getStatistics().clear();

			for (EmployeeGroup group : loadedGroups) {
				assertTrue( Hibernate.isInitialized( group.getManager().getCollaborators() ) );
			}

			assertEquals( 0, sessionFactory.getStatistics().getPrepareStatementCount() );

			assertEquals( loadedGroups[0].getLead().getCollaborators().size(), loadedGroups[0].getLead().getCollaborators().size() );
			assertEquals( loadedGroups[1].getLead().getCollaborators().size(), loadedGroups[1].getLead().getCollaborators().size() );
			assertEquals( loadedGroups[0].getManager().getCollaborators().size(), loadedGroups[0].getManager().getCollaborators().size() );
			assertEquals( loadedGroups[1].getManager().getCollaborators().size(), loadedGroups[1].getManager().getCollaborators().size() );

			assertEquals( 0, sessionFactory.getStatistics().getPrepareStatementCount() );
		} );
	}

	@Entity(name = "EmployeeGroup")
	@Table(name = "EmployeeGroup")
	@BatchSize(size = 1000)
	public static class EmployeeGroup {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		private Employee manager;

		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		private Employee lead;

		@OneToMany(cascade = CascadeType.ALL)
		@Fetch(FetchMode.SUBSELECT)
		@JoinTable(name="EmployeeGroup_employees")
		private List<Employee> employees = new ArrayList<>();

		public EmployeeGroup(long id) {
			this.id = id;
		}

		@SuppressWarnings("unused")
		protected EmployeeGroup() {
		}

		public Employee getManager() {
			return manager;
		}

		public void setManager(Employee manager) {
			this.manager = manager;
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

		public long getId() {
			return id;
		}

		@Override
		public String toString() {
			return id.toString();
		}
	}


	@Entity(name="Employee")
	@Table(name = "Employee")
	public static class Employee {
		@Id
		@GeneratedValue
		private Long id;
		private String name;

		@OneToMany(cascade = CascadeType.ALL)
		@Fetch(FetchMode.SUBSELECT)
		private List<Employee> collaborators = new ArrayList<>();

		public String getName() {
			return name;
		}

		@SuppressWarnings("unused")
		private Employee() {
		}

		public Employee(String name) {
			this.name = name;
		}

		public boolean addCollaborator(Employee employee) {
			return collaborators.add(employee);
		}

		public List<Employee> getCollaborators() {
			return collaborators;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
