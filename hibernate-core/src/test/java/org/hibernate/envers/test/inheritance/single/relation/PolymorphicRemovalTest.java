/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.inheritance.single.relation;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Tests that after the removal of an entity that maintains a polymorphic relation that
 * the {@code AuditReader} queries return the correct polymorphic type for revisions.
 * <p/>
 * Previously, this test would have returned {@link EmployeeType} when looking up the
 * entity associated to revision 3 of typeId; however after the fix it properly will
 * return {@link SalaryEmployeeType} instances instead.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7249")
@Disabled("NYI - Inheritance")
public class PolymorphicRemovalTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer typeId;
	private Integer employeeId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Employee.class, EmployeeType.class, SalaryEmployeeType.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					SalaryEmployeeType type = new SalaryEmployeeType();
					type.setData( "salaried" );
					entityManager.persist( type );

					this.typeId = type.getId();
				},

				// Revision 2
				entityManager -> {
					EmployeeType type = entityManager.find( EmployeeType.class, typeId );
					Employee employee = new Employee();
					employee.setType( type );
					entityManager.persist( employee );

					this.employeeId = employee.getId();
				},

				// Revision 3
				entityManager -> {
					Employee employee = entityManager.find( Employee.class, employeeId );
					entityManager.remove( employee );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( EmployeeType.class, typeId ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( Employee.class, employeeId ), contains( 2, 3 ) );
	}

	@DynamicTest
	public void testRevisionHistoryPayment() {
		final EmployeeType rev1 = getAuditReader().find( EmployeeType.class, typeId, 1 );
		assertThat( rev1, instanceOf( SalaryEmployeeType.class ) );
		assertThat( rev1.getType(), equalTo( "SALARY" ) );

		final EmployeeType rev2 = getAuditReader().find( EmployeeType.class, typeId, 2 );
		assertThat( rev2, instanceOf( SalaryEmployeeType.class ) );
		assertThat( rev2.getType(), equalTo( "SALARY" ) );

		final EmployeeType rev3 = getAuditReader().find( EmployeeType.class, typeId, 3 );
		assertThat( rev3, instanceOf( SalaryEmployeeType.class ) );
		assertThat( rev3.getType(), equalTo( "SALARY" ) );
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
