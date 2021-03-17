/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.group;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests removing non-owning side of the bidirectional association,
 * where owning side is in an embeddable.
 *
 * Tests with and without dirty-checking using enhancement.
 *
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-13241")
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({
		EnhancerTestContext.class,
		BidirectionalLazyGroupsInEmbeddableTest.NoDirtyCheckEnhancementContext.class
})
public class BidirectionalLazyGroupsInEmbeddableTest extends BaseCoreFunctionalTestCase {

	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employer.class, Employee.class };
	}

	@Test
	@Ignore("Test is failing with Javassist and also fails with ByteBuddy if the mappings are moved to the fields.")
	public void test() {

		doInHibernate(
				this::sessionFactory, session -> {

					Employer employer = new Employer( "RedHat" );
					session.persist( employer );
					employer.addEmployee( new Employee( "Jack" ) );
					employer.addEmployee( new Employee( "Jill" ) );
					employer.addEmployee( new Employee( "John" ) );
					for ( Employee employee : employer.getEmployees() ) {
						session.persist( employee );
					}
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					Employer employer = session.createQuery( "from Employer e", Employer.class ).getSingleResult();
					session.remove( employer );
					for ( Employee employee : employer.getEmployees() ) {
						session.remove( employee );
					}
				}
		);
	}

	@Entity(name = "Employer")
	public static class Employer {
		private String name;

		private Set<Employee> employees;

		public Employer(String name) {
			this();
			setName( name );
		}

		@Id
		public String getName() {
			return name;
		}

		@OneToMany(mappedBy = "employerContainer.employer", fetch = FetchType.LAZY)
		@LazyGroup("Employees")
		public Set<Employee> getEmployees() {
			return employees;
		}

		public void addEmployee(Employee employee) {
			if ( getEmployees() == null ) {
				setEmployees( new HashSet<>() );
			}
			employees.add( employee );
			if ( employee.getEmployerContainer() == null ) {
				employee.setEmployerContainer( new EmployerContainer() );
			}
			employee.getEmployerContainer().setEmployer( this );
		}

		protected Employer() {
			// this form used by Hibernate
		}

		protected void setName(String name) {
			this.name = name;
		}

		protected void setEmployees(Set<Employee> employees) {
			this.employees = employees;
		}
	}

	@Entity(name = "Employee")
	public static class Employee {
		private long id;

		private String name;

		public Employee(String name) {
			this();
			setName( name );
		}

		@Id
		@GeneratedValue
		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public EmployerContainer employerContainer;

		protected Employee() {
			// this form used by Hibernate
		}

		public EmployerContainer getEmployerContainer() {
			return employerContainer;
		}

		protected void setId(long id) {
			this.id = id;
		}

		protected void setName(String name) {
			this.name = name;
		}

		protected void setEmployerContainer(EmployerContainer employerContainer) {
			this.employerContainer = employerContainer;
		}

		public int hashCode() {
			if ( name != null ) {
				return name.hashCode();
			}
			else {
				return 0;
			}
		}

		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			else if ( o instanceof Employee ) {
				Employee other = Employee.class.cast( o );
				if ( name != null ) {
					return getName().equals( other.getName() );
				}
				else {
					return other.getName() == null;
				}
			}
			else {
				return false;
			}
		}
	}

	@Embeddable
	public static class EmployerContainer {
		private Employer employer;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@LazyGroup("EmployerForEmployee")
		@JoinColumn(name = "employer_name")
		public Employer getEmployer() {
			return employer;
		}

		protected void setEmployer(Employer employer) {
			this.employer = employer;
		}
	}

	public static class NoDirtyCheckEnhancementContext extends DefaultEnhancementContext {
		@Override
		public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
			return false;
		}
	}
}
