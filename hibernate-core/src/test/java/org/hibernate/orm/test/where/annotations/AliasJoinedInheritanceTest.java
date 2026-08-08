/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel( annotatedClasses = {
	AliasJoinedInheritanceTest.Company.class,
	AliasJoinedInheritanceTest.Person.class,
	AliasJoinedInheritanceTest.Employee.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-12016" )
public class AliasJoinedInheritanceTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var company = new Company( 1L, "company" );
			var named = new Employee( 2L, "named employee", "senior" );
			var unnamed = new Employee( 3L, null, "junior" );
			var untitled = new Employee( 4L, "untitled employee", null );
			var inactive = new Employee( 5L, "inactive employee", "principal", false );

			company.employees.add( named );
			company.employees.add( unnamed );
			company.employees.add( untitled );

			company.people.add( named );
			company.people.add( unnamed );
			company.people.add( untitled );
			company.people.add( inactive );

			company.titledEmployees.add( named );
			company.titledEmployees.add( unnamed );
			company.titledEmployees.add( untitled );

			company.namedAndTitledEmployees.add( named );
			company.namedAndTitledEmployees.add( unnamed );
			company.namedAndTitledEmployees.add( untitled );

			company.tableAliasedEmployees.add( named );
			company.tableAliasedEmployees.add( unnamed );
			company.tableAliasedEmployees.add( untitled );

			company.contractors.add( named );
			company.contractors.add( unnamed );
			company.contractors.add( untitled );

			company.directEmployees.add( named );
			company.directEmployees.add( unnamed );
			company.directEmployees.add( untitled );

			named.employer = company;
			unnamed.employer = company;
			untitled.employer = company;

			session.persist( company );
			session.persist( named );
			session.persist( unnamed );
			session.persist( untitled );
			session.persist( inactive );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testRootEntityAliasWithJoinTable(SessionFactoryScope factoryScope) {
		final var statementInspector = factoryScope.getCollectingStatementInspector();
		statementInspector.clear();
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertNotNull( company );
			assertEquals( 2, company.employees.size() );
		} );
		assertAliasesInterpolated( statementInspector );
	}

	@Test
	public void testJoinFetch(SessionFactoryScope factoryScope) {
		final var statementInspector = factoryScope.getCollectingStatementInspector();
		statementInspector.clear();
		factoryScope.inTransaction( (session) -> {
			var company = session.createQuery(
			"from Company c left join fetch c.employees where c.id = :id",
					Company.class
				)
				.setParameter( "id", 1L )
				.getSingleResult();
			assertEquals( 2, company.employees.size() );
		} );
		assertAliasesInterpolated( statementInspector );
	}

	@Test
	public void testSubclassEntityAlias(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 2, company.titledEmployees.size() );
			assertTrue( company.titledEmployees.stream().allMatch( employee -> employee.title != null ) );
		} );
	}

	@Test
	public void testMultipleAliases(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 1, company.namedAndTitledEmployees.size() );
			var employee = company.namedAndTitledEmployees.iterator().next();
			assertNotNull( employee.getName() );
			assertNotNull( employee.title );
		} );
	}

	@Test
	public void testTableAlias(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 2, company.tableAliasedEmployees.size() );
			assertTrue( company.tableAliasedEmployees.stream().allMatch( employee -> employee.getName() != null ) );
		} );
	}

	@Test
	public void testManyToMany(SessionFactoryScope factoryScope) {
		final var statementInspector = factoryScope.getCollectingStatementInspector();
		statementInspector.clear();
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 2, company.contractors.size() );
			assertTrue( company.contractors.stream().allMatch( employee -> employee.getName() != null ) );
		} );
		assertAliasesInterpolated( statementInspector );
	}

	@Test
	public void testOneToManyWithoutJoinTable(SessionFactoryScope factoryScope) {
		final var statementInspector = factoryScope.getCollectingStatementInspector();
		statementInspector.clear();
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 2, company.directEmployees.size() );
			assertTrue( company.directEmployees.stream().allMatch( employee -> employee.title != null ) );
		} );
		assertAliasesInterpolated( statementInspector );
	}

	@Test
	public void testOneToManyMappedBy(SessionFactoryScope factoryScope) {
		final var statementInspector = factoryScope.getCollectingStatementInspector();
		statementInspector.clear();
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 2, company.staff.size() );
			assertTrue( company.staff.stream().allMatch( employee -> employee.getName() != null ) );
		} );
		assertAliasesInterpolated( statementInspector );
	}

	@Test
	public void testMixedAliasAndNonAliasRestrictions(SessionFactoryScope factoryScope) {
		final var statementInspector = factoryScope.getCollectingStatementInspector();
		statementInspector.clear();
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			// the aliased restriction on the collection and the plain restriction
			// on the Person class are combined: the inactive person satisfies
			// "{p}.name is not null" but is excluded by "active = true"
			assertEquals( 2, company.people.size() );
			assertTrue( company.people.stream().allMatch( person -> person.getName() != null ) );
			assertTrue( company.people.stream().noneMatch( person -> "inactive employee".equals( person.getName() ) ) );
		} );
		assertAliasesInterpolated( statementInspector );
	}

	@Test
	public void testCollectionMutation(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 2, company.employees.size() );
			company.employees.removeIf( employee -> "named employee".equals( employee.getName() ) );
		} );

		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 1, company.employees.size() );
			assertEquals( "untitled employee", company.employees.iterator().next().getName() );
		} );
	}

	private static void assertAliasesInterpolated(org.hibernate.testing.jdbc.SQLStatementInspector statementInspector) {
		for ( String sql : statementInspector.getSqlQueries() ) {
			assertFalse( sql.contains( "{p}" ) || sql.contains( "{e}" ),
					"alias placeholder was not interpolated in: " + sql );
		}
	}

	@Entity(name = "Company")
	public static class Company {
		@Id
		private Long id;
		private String name;

		@OneToMany
		@SQLRestriction(
			value = "{p}.name is not null",
			aliases = @SqlFragmentAlias( alias = "p", entity = Person.class )
		)
		private Set<Employee> employees = new HashSet<>();

		@OneToMany
		@JoinTable(name = "company_titled_employees")
		@SQLRestriction(
			value = "{e}.title is not null",
			aliases = @SqlFragmentAlias( alias = "e", entity = Employee.class )
		)
		private Set<Employee> titledEmployees = new HashSet<>();

		@OneToMany
		@JoinTable(name = "company_named_titled")
		@SQLRestriction(
			value = "{p}.name is not null and {e}.title is not null",
			aliases = {
				@SqlFragmentAlias( alias = "p", entity = Person.class ),
				@SqlFragmentAlias( alias = "e", entity = Employee.class )
			}
		)
		private Set<Employee> namedAndTitledEmployees = new HashSet<>();

		@OneToMany
		@JoinTable(name = "company_table_aliased")
		@SQLRestriction(
			value = "{p}.name is not null",
			aliases = @SqlFragmentAlias( alias = "p", table = "Person" )
		)
		private Set<Employee> tableAliasedEmployees = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "company_contractors")
		@SQLRestriction(
			value = "{p}.name is not null",
			aliases = @SqlFragmentAlias( alias = "p", entity = Person.class )
		)
		private Set<Employee> contractors = new HashSet<>();

		@OneToMany
		@JoinColumn(name = "company_id")
		@SQLRestriction(
			value = "{e}.title is not null",
			aliases = @SqlFragmentAlias( alias = "e", entity = Employee.class )
		)
		private Set<Employee> directEmployees = new HashSet<>();

		@OneToMany(mappedBy = "employer")
		@SQLRestriction(
			value = "{p}.name is not null",
			aliases = @SqlFragmentAlias( alias = "p", entity = Person.class )
		)
		private Set<Employee> staff = new HashSet<>();

		// combined with the plain "active = true" restriction on the Person class
		@OneToMany
		@JoinTable(name = "company_people")
		@SQLRestriction(
			value = "{p}.name is not null",
			aliases = @SqlFragmentAlias( alias = "p", entity = Person.class )
		)
		private Set<Person> people = new HashSet<>();

		public Company() {
		}
		public Company(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	@SQLRestriction("active = true")
	public static class Person {
		@Id
		private Long id;
		private String name;
		private boolean active = true;

		public Person() {
		}
		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}
		public Person(Long id, String name, boolean active) {
			this( id, name );
			this.active = active;
		}
		public String getName() {
			return name;
		}
	}

	@Entity(name = "Employee")
	public static class Employee extends Person {
		private String title;

		@ManyToOne(fetch = FetchType.LAZY)
		private Company employer;

		public Employee() {
		}
		public Employee(Long id, String name, String title) {
			super( id, name );
			this.title = title;
		}
		public Employee(Long id, String name, String title, boolean active) {
			super( id, name, active );
			this.title = title;
		}
	}
}
