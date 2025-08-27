/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.version;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		InheritanceImplicitVersionUpdateTest.ObjectWithUnid.class,
		InheritanceImplicitVersionUpdateTest.Employee.class,
		InheritanceImplicitVersionUpdateTest.CustomEmployee.class,
		InheritanceImplicitVersionUpdateTest.Company.class,
		InheritanceImplicitVersionUpdateTest.CustomCompany.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16237" )
public class InheritanceImplicitVersionUpdateTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CustomEmployee employee = new CustomEmployee( "Vittorio" );
			session.persist( employee );
			final CustomCompany company = new CustomCompany();
			company.setName( "Test Company" );
			company.getEmployees().add( employee );
			session.persist( company );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Employee" ).executeUpdate();
			session.createMutationQuery( "delete from Company" ).executeUpdate();
		} );
	}

	@Test
	public void testUpdateElementCollection(SessionFactoryScope scope) {
		final Long version = scope.fromTransaction( session -> {
			final CustomCompany company = session.find( CustomCompany.class, 1L );
			company.getStandardFiles().put( "file_1", "First file" );
			return company.getVersion();
		} );
		assertVersionIncreased( scope, "CustomCompany", version );
	}

	@Test
	public void testUpdateAssociatedCollection(SessionFactoryScope scope) {
		final Long version = scope.fromTransaction( session -> {
			final CustomCompany company = session.find( CustomCompany.class, 1L );
			company.getEmployees().clear();
			return company.getVersion();
		} );
		assertVersionIncreased( scope, "CustomCompany", version );
	}

	@Test
	public void testUpdateBasicProperty(SessionFactoryScope scope) {
		final Long version = scope.fromTransaction( session -> {
			final CustomCompany company = session.find( CustomCompany.class, 1L );
			company.setCustomProperty( "Custom" );
			return company.getVersion();
		} );
		assertVersionIncreased( scope, "CustomCompany", version );
	}

	@Test
	public void testUpdateBasicSuperclassProperty(SessionFactoryScope scope) {
		final Long version = scope.fromTransaction( session -> {
			final CustomCompany company = session.find( CustomCompany.class, 1L );
			company.setName( "Updated Company" );
			return company.getVersion();
		} );
		assertVersionIncreased( scope, "CustomCompany", version );
	}

	@Test
	public void testUpdateVersionProperty(SessionFactoryScope scope) {
		final Long version = scope.fromTransaction( session -> {
			final CustomEmployee customEmployee = session.find( CustomEmployee.class, 1L );
			final Long originalVersion = customEmployee.getVersion();
			customEmployee.setVersion( 3L ); // this value is ignored and version is increased by 1
			return originalVersion;
		} );
		assertVersionIncreased( scope, "CustomEmployee", version );
	}

	private void assertVersionIncreased(SessionFactoryScope scope, String entityName, Long previousVersion) {
		scope.inTransaction( session -> assertThat(
				session.createQuery(
						String.format( "select version from %s", entityName ),
						Long.class
				).getSingleResult()
		).isEqualTo( previousVersion + 1 ) );
	}

	@MappedSuperclass
	public static class ObjectWithUnid {
		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Long version;

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}

	@Entity( name = "Employee" )
	public static class Employee extends ObjectWithUnid {
		private String name;

		public Employee() {
		}

		public Employee(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "CustomEmployee" )
	public static class CustomEmployee extends Employee {
		private String customProperty;

		public CustomEmployee() {
		}

		public CustomEmployee(String name) {
			super( name );
		}

		public String getCustomProperty() {
			return customProperty;
		}

		public void setCustomProperty(String customProperty) {
			this.customProperty = customProperty;
		}
	}


	@Entity( name = "Company" )
	public static class Company extends ObjectWithUnid {
		private String name;

		@ElementCollection( fetch = FetchType.LAZY )
		private Map<String, String> standardFiles;

		@OneToMany
		@JoinColumn( name = "company_id" )
		private Set<Employee> employees;

		public Company() {
			this.standardFiles = new HashMap<>();
			this.employees = new HashSet<>();
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Map<String, String> getStandardFiles() {
			return standardFiles;
		}

		public Set<Employee> getEmployees() {
			return employees;
		}
	}

	@Entity( name = "CustomCompany" )
	public static class CustomCompany extends Company {
		private String customProperty;

		public String getCustomProperty() {
			return customProperty;
		}

		public void setCustomProperty(String customProperty) {
			this.customProperty = customProperty;
		}
	}
}
