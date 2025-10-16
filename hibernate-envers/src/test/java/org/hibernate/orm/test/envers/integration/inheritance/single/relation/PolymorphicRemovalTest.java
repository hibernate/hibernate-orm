/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.single.relation;

import java.util.Arrays;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that after the removal of an entity that maintains a polymorphic relation that
 * the {@code AuditReader} queries return the correct polymorphic type for revisions.
 * <p>
 * Previously, this test would have returned {@link EmployeeType} when looking up the
 * entity associated to revision 3 of typeId; however after the fix it properly will
 * return {@link SalaryEmployeeType} instances instead.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-7249")
@EnversTest
@Jpa(annotatedClasses = {
		PolymorphicRemovalTest.Employee.class,
		PolymorphicRemovalTest.EmployeeType.class,
		PolymorphicRemovalTest.SalaryEmployeeType.class
})
public class PolymorphicRemovalTest {
	private Integer typeId;
	private Integer employeeId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// revision 1
		this.typeId = scope.fromTransaction( em -> {
			SalaryEmployeeType type = new SalaryEmployeeType();
			type.setData( "salaried" );
			em.persist( type );
			return type.getId();
		} );
		// revision 2
		this.employeeId = scope.fromTransaction( em -> {
			EmployeeType type = em.find( EmployeeType.class, typeId );
			Employee employee = new Employee();
			employee.setType( type );
			em.persist( employee );
			return employee.getId();
		} );
		// revision 3
		scope.inTransaction( em -> {
			Employee employee = em.find( Employee.class, employeeId );
			em.remove( employee );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( EmployeeType.class, typeId ) );
			assertEquals( Arrays.asList( 2, 3 ), auditReader.getRevisions( Employee.class, employeeId ) );
		} );
	}

	@Test
	public void testRevisionHistoryPayment(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final EmployeeType rev1 = auditReader.find( EmployeeType.class, typeId, 1 );
			assertTyping( SalaryEmployeeType.class, rev1 );
			assertEquals( "SALARY", rev1.getType() );
			final EmployeeType rev2 = auditReader.find( EmployeeType.class, typeId, 2 );
			assertTyping( SalaryEmployeeType.class, rev2 );
			assertEquals( "SALARY", rev2.getType() );
			final EmployeeType rev3 = auditReader.find( EmployeeType.class, typeId, 3 );
			assertTyping( SalaryEmployeeType.class, rev3 );
			assertEquals( "SALARY", rev3.getType() );
		} );
	}

	@Entity(name = "EmployeeType")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "TYPE")
	@DiscriminatorValue("UNKNOWN")
	@Audited
	public static class EmployeeType {
		@Id
		@GeneratedValue
		private Integer id;
		@OneToMany(mappedBy = "type")
		private Set<Employee> employees;
		// used to expose the discriminator value for assertion checking
		@Column(name = "TYPE", insertable = false, updatable = false, nullable = false, length = 31)
		private String type;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Set<Employee> getEmployees() {
			return employees;
		}

		public void setEmployees(Set<Employee> employees) {
			this.employees = employees;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}

	@Entity(name = "SalaryEmployee")
	@DiscriminatorValue("SALARY")
	@Audited
	public static class SalaryEmployeeType extends EmployeeType {
		private String data;

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Entity(name = "Employee")
	@Audited
	public static class Employee {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		@ManyToOne(fetch = FetchType.LAZY)
		private EmployeeType type;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public EmployeeType getType() {
			return type;
		}

		public void setType(EmployeeType type) {
			this.type = type;
		}
	}
}
