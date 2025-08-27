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
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

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
public class PolymorphicRemovalTest extends BaseEnversJPAFunctionalTestCase {
	private Integer typeId;
	private Integer employeeId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Employee.class, EmployeeType.class, SalaryEmployeeType.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// revision 1
		this.typeId = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			SalaryEmployeeType type = new SalaryEmployeeType();
			type.setData( "salaried" );
			entityManager.persist( type );
			return type.getId();
		} );
		// revision 2
		this.employeeId = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			EmployeeType type = entityManager.find( EmployeeType.class, typeId );
			Employee employee = new Employee();
			employee.setType( type );
			entityManager.persist( employee );
			return employee.getId();
		} );
		// revision 3
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = entityManager.find( Employee.class, employeeId );
			entityManager.remove( employee );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals(Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions( EmployeeType.class, typeId ) );
		assertEquals( Arrays.asList( 2, 3 ), getAuditReader().getRevisions( Employee.class, employeeId ) );
	}

	@Test
	public void testRevisionHistoryPayment() {
		final EmployeeType rev1 = getAuditReader().find( EmployeeType.class, typeId, 1 );
		assertTyping( SalaryEmployeeType.class, rev1 );
		assertEquals( "SALARY", rev1.getType() );
		final EmployeeType rev2 = getAuditReader().find( EmployeeType.class, typeId, 2 );
		assertTyping( SalaryEmployeeType.class, rev2 );
		assertEquals( "SALARY", rev2.getType() );
		final EmployeeType rev3 = getAuditReader().find( EmployeeType.class, typeId, 3 );
		assertTyping( SalaryEmployeeType.class, rev3 );
		assertEquals( "SALARY", rev3.getType() );
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
