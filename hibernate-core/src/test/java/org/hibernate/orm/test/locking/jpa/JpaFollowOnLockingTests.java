/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.locking.jpa;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernateHints;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.RollbackException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = { Employee.class, Department.class })
public class JpaFollowOnLockingTests {
	@Test
	public void testQueryLockingBaseline(EntityManagerFactoryScope scope) {
		performQueryLocking( scope, null );
	}

	@Test
	public void testQueryLocking(EntityManagerFactoryScope scope) {
		performQueryLocking( scope, true );
	}

	public void performQueryLocking(EntityManagerFactoryScope scope, Boolean followOnLocking) {
		scope.inTransaction( (em) -> {
			final List<Employee> employees = em.createQuery( "select e from Employee e where e.salary > 10", Employee.class )
					.setLockMode( LockModeType.PESSIMISTIC_READ )
					.setHint( HibernateHints.HINT_FOLLOW_ON_LOCKING, followOnLocking )
					.getResultList();
			assertThat( employees ).hasSize( 1 );
			final LockModeType appliedLockMode = em.getLockMode( employees.get( 0 ) );
			assertThat( appliedLockMode ).isIn( LockModeType.PESSIMISTIC_READ, LockModeType.PESSIMISTIC_WRITE );

			try {
				// with the initial txn still active (locks still held), try to update the row from another txn
				scope.inTransaction( (session2) -> {
					final Employee june = session2.find( Employee.class, 3 );
					june.setSalary( 90000F );
				} );
				fail( "Locked entity update was allowed" );
			}
			catch (PessimisticLockException | LockTimeoutException | RollbackException expected) {
			}
		} );
	}

	@BeforeEach
	public void createTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Department engineering = new Department( 1, "Engineering" );
			session.persist( engineering );

			session.persist( new Employee( 1, "John", 9F, engineering ) );
			session.persist( new Employee( 2, "Mary", 10F, engineering ) );
			session.persist( new Employee( 3, "June", 11F, engineering ) );
		} );
	}

	@AfterEach
	public void dropTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "delete Employee" ).executeUpdate();
			session.createQuery( "delete Department" ).executeUpdate();
		} );
	}
}
