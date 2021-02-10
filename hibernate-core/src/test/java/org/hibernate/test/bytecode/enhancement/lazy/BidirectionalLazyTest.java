/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests removing non-owning side of the bidirectional association,
 * with and without dirty-checking using enhancement.
 *
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-13241")
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({
		EnhancerTestContext.class, // supports laziness and dirty-checking
		BidirectionalLazyTest.NoDirtyCheckEnhancementContext.class // supports laziness; does not support dirty-checking
})
public class BidirectionalLazyTest extends BaseCoreFunctionalTestCase {

	// NOTE : tests in this class seem redundant because they assert things that happened
	// in previous versions that have been fixed

	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employer.class, Employee.class, Unrelated.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Test
	public void testRemoveWithDeletedAssociatedEntity() {

		inTransaction(
				(session) -> {

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

		inTransaction(
				(session) -> {
					Employer employer = session.get( Employer.class, "RedHat" );

					// Delete the associated entity first
					session.remove( employer );

					for ( Employee employee : employer.getEmployees() ) {
						assertTrue( Hibernate.isPropertyInitialized( employee, "employer" ) );
						session.remove( employee );

						assertSame( employer, employee.getEmployer() );

						// employee.getEmployer was initialized, and should be nullified in EntityEntry#deletedState
						checkEntityEntryState( session, employee, employer, true );
					}
				}
		);

		inTransaction(
				(session) -> {
					assertNull( session.find( Employer.class, "RedHat" ) );
					assertTrue( session.createQuery( "from Employee e", Employee.class ).getResultList().isEmpty() );
				}
		);
	}

	@Test
	public void testRemoveWithNonAssociatedRemovedEntity() {

		inTransaction(
				(session) -> {
					Employer employer = new Employer( "RedHat" );
					session.persist( employer );
					Employee employee = new Employee( "Jack" );
					employer.addEmployee( employee );
					session.persist( employee );
					session.persist( new Unrelated( 1 ) );
				}
		);

		inTransaction(
				(session) -> {
					// Delete an entity that is not associated with Employee
					session.remove( session.get( Unrelated.class, 1 ) );
					final Employee employee = session.get( Employee.class, "Jack" );
					assertTrue( Hibernate.isPropertyInitialized( employee, "employer" ) );

					session.remove( employee );

					// employee.getEmployer was initialized, and should not be nullified in EntityEntry#deletedState
					checkEntityEntryState( session, employee, employee.getEmployer(), false );
				}
		);

		inTransaction(
				(session) -> {
					assertNull( session.find( Unrelated.class, 1 ) );
					assertNull( session.find( Employee.class, "Jack" ) );
					session.remove( session.find( Employer.class, "RedHat" ) );
				}
		);
	}

	@Test
	public void testRemoveWithNoRemovedEntities() {

		inTransaction(
				(session) -> {
					Employer employer = new Employer( "RedHat" );
					session.persist( employer );
					Employee employee = new Employee( "Jack" );
					employer.addEmployee( employee );
					session.persist( employee );
				}
		);

		inTransaction(
				(session) -> {
					// Don't delete any entities before deleting the Employee
					final Employee employee = session.get( Employee.class, "Jack" );
					verifyBaseState( employee );

					session.remove( employee );

					Employer employer = session.get( Employer.class, "RedHat" );
					verifyBaseState( employer );

					assertThat( employee.getEmployer(), sameInstance( employer ) );

					checkEntityEntryState( session, employee, employer, false );
				}
		);

		inTransaction(
				(session) -> {
					assertNull( session.find( Employee.class, "Jack" ) );
					session.remove( session.find( Employer.class, "RedHat" ) );
				}
		);
	}

	@Test
	public void testRemoveEntityWithNullLazyManyToOne() {

		inTransaction(
				(session) -> {
					Employer employer = new Employer( "RedHat" );
					session.persist( employer );
					Employee employee = new Employee( "Jack" );
					session.persist( employee );
				}
		);

		inTransaction(
				(session) -> {
					Employee employee = session.get( Employee.class, "Jack" );
					verifyBaseState( employee );

					// Get and delete an Employer that is not associated with employee
					Employer employer = session.get( Employer.class, "RedHat" );
					verifyBaseState( employer );

					session.remove( employer );
					session.remove( employee );
				}
		);
	}

	/**
	 * @implSpec Same as {@link #testRemoveEntityWithNullLazyManyToOne} but
	 * deleting the Employer linked to the loaded Employee
	 */
	@Test
	public void testRemoveEntityWithLinkedLazyManyToOne() {
		inTransaction(
				session -> {
					Employer employer = new Employer( "RedHat" );
					session.persist( employer );
					Employee employee = new Employee( "Jack" );
					employee.setEmployer( employer );
					session.persist( employee );
				}
		);

		inTransaction(
				session -> {
					Employee employee = session.get( Employee.class, "Jack" );
					verifyBaseState( employee );

					// Get and delete an Employer that is not associated with employee
					Employer employer = session.get( Employer.class, "RedHat" );
					verifyBaseState( employer );

					assertThat( employee.getEmployer(), sameInstance( employer ) );

					session.remove( employer );
					session.remove( employee );
				}
		);
	}

	private void verifyBaseState(Employer employer) {
		assertTrue( Hibernate.isPropertyInitialized( employer, "name" ) );
	}

	private void verifyBaseState(Employee employee) {
		assertTrue( Hibernate.isPropertyInitialized( employee, "name" ) );

		// employer will be either null or an uninitialized enhanced-proxy
		assertTrue( Hibernate.isPropertyInitialized( employee, "employer" ) );

		final Employer employer = employee.getEmployer();
		if ( employer != null ) {
			assertFalse( Hibernate.isInitialized( employer ) );
			assertThat(employer, not( instanceOf( HibernateProxy.class ) ) );
		}
	}

	private void checkEntityEntryState(
			final Session session,
			final Employee employee,
			final Object employer,
			final boolean isEmployerNullified) {
		final SessionImplementor sessionImplementor = (SessionImplementor) session;
		final EntityEntry entityEntry = sessionImplementor.getPersistenceContext().getEntry( employee );
		final int propertyNumber = entityEntry.getPersister().getEntityMetamodel().getPropertyIndex( "employer" );
		assertEquals(
				employer,
				entityEntry.getLoadedState()[propertyNumber]
		);
		if ( isEmployerNullified ) {
			assertNull( entityEntry.getDeletedState()[ propertyNumber ] );
		}
		else {
			assertEquals(
					employer,
					entityEntry.getDeletedState()[propertyNumber]
			);
		}
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

		@OneToMany(mappedBy = "employer", fetch = FetchType.LAZY)
		public Set<Employee> getEmployees() {
			return employees;
		}

		public void addEmployee(Employee employee) {
			if ( getEmployees() == null ) {
				setEmployees( new HashSet<>() );
			}
			employees.add( employee );
			employee.setEmployer( this );
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

		private Employer employer;

		public Employee(String name) {
			this();
			setName( name );
		}

		public long getId() {
			return id;
		}

		@Id
		public String getName() {
			return name;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@JoinColumn(name = "employer_name")
		public Employer getEmployer() {
			return employer;
		}

		protected Employee() {
			// this form used by Hibernate
		}

		protected void setId(long id) {
			this.id = id;
		}

		protected void setName(String name) {
			this.name = name;
		}

		protected void setEmployer(Employer employer) {
			this.employer = employer;
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
				Employee other = (Employee) o;
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

	@Entity(name = "Manager")
	public static class Manager extends Employee {
	}

	@Entity(name = "Unrelated")
	public static class Unrelated {
		private int id;

		public Unrelated() {
		}

		public Unrelated(int id) {
			setId( id );
		}

		@Id
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
	}

	public static class NoDirtyCheckEnhancementContext extends DefaultEnhancementContext {
		@Override
		public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
			return true;
		}

		@Override
		public boolean isLazyLoadable(UnloadedField field) {
			return true;
		}

		@Override
		public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
			return false;
		}
	}
}
