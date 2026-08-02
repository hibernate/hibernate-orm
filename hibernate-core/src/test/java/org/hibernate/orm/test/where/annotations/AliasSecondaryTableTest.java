/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SecondaryTable;
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

@SessionFactory(useCollectingStatementInspector = true)
@DomainModel( annotatedClasses = {
	AliasSecondaryTableTest.Company.class,
	AliasSecondaryTableTest.Employee.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-12016" )
public class AliasSecondaryTableTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var company = new Company( 1L, "company" );
			var named = new Employee( 2L, "named employee", "senior" );
			var unnamed = new Employee( 3L, null, "junior" );
			var untitled = new Employee( 4L, "untitled employee", null );

			company.employees.add( named );
			company.employees.add( unnamed );
			company.employees.add( untitled );

			company.namedAndTitledEmployees.add( named );
			company.namedAndTitledEmployees.add( unnamed );
			company.namedAndTitledEmployees.add( untitled );

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
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testSecondaryTableAliasWithJoinTable(SessionFactoryScope factoryScope) {
		final var statementInspector = factoryScope.getCollectingStatementInspector();
		statementInspector.clear();
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertNotNull( company );
			assertEquals( 2, company.employees.size() );
			assertTrue( company.employees.stream().allMatch( employee -> employee.title != null ) );
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
	public void testMultipleAliases(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 1, company.namedAndTitledEmployees.size() );
			var employee = company.namedAndTitledEmployees.iterator().next();
			assertNotNull( employee.name );
			assertNotNull( employee.title );
		} );
	}

	@Test
	public void testManyToMany(SessionFactoryScope factoryScope) {
		final var statementInspector = factoryScope.getCollectingStatementInspector();
		statementInspector.clear();
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 2, company.contractors.size() );
			assertTrue( company.contractors.stream().allMatch( employee -> employee.title != null ) );
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
			assertTrue( company.directEmployees.stream().allMatch( employee -> employee.name != null ) );
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
			assertTrue( company.staff.stream().allMatch( employee -> employee.title != null ) );
		} );
		assertAliasesInterpolated( statementInspector );
	}

	@Test
	public void testCollectionMutation(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 2, company.employees.size() );
			company.employees.removeIf( employee -> "named employee".equals( employee.name ) );
		} );

		factoryScope.inTransaction( (session) -> {
			var company = session.find( Company.class, 1L );
			assertEquals( 1, company.employees.size() );
			assertEquals( "junior", company.employees.iterator().next().title );
		} );
	}

	private static void assertAliasesInterpolated(org.hibernate.testing.jdbc.SQLStatementInspector statementInspector) {
		for ( String sql : statementInspector.getSqlQueries() ) {
			assertFalse( sql.contains( "{e}" ) || sql.contains( "{d}" ),
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
			value = "{d}.title is not null",
			aliases = @SqlFragmentAlias( alias = "d", table = "employee_details" )
		)
		private Set<Employee> employees = new HashSet<>();

		@OneToMany
		@JoinTable(name = "company_named_titled")
		@SQLRestriction(
			value = "{e}.name is not null and {d}.title is not null",
			aliases = {
				@SqlFragmentAlias( alias = "e", entity = Employee.class ),
				@SqlFragmentAlias( alias = "d", table = "employee_details" )
			}
		)
		private Set<Employee> namedAndTitledEmployees = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "company_contractors")
		@SQLRestriction(
			value = "{d}.title is not null",
			aliases = @SqlFragmentAlias( alias = "d", table = "employee_details" )
		)
		private Set<Employee> contractors = new HashSet<>();

		@OneToMany
		@JoinColumn(name = "company_id")
		@SQLRestriction(
			value = "{e}.name is not null",
			aliases = @SqlFragmentAlias( alias = "e", entity = Employee.class )
		)
		private Set<Employee> directEmployees = new HashSet<>();

		@OneToMany(mappedBy = "employer")
		@SQLRestriction(
			value = "{d}.title is not null",
			aliases = @SqlFragmentAlias( alias = "d", table = "employee_details" )
		)
		private Set<Employee> staff = new HashSet<>();

		public Company() {
		}
		public Company(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Employee")
	@SecondaryTable(name = "employee_details")
	public static class Employee {
		@Id
		private Long id;
		private String name;

		@Column(table = "employee_details")
		private String title;

		@ManyToOne(fetch = FetchType.LAZY)
		private Company employer;

		public Employee() {
		}
		public Employee(Long id, String name, String title) {
			this.id = id;
			this.name = name;
			this.title = title;
		}
	}
}
