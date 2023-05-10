/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.locking.jpa;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(settings = @Setting(name = AvailableSettings.JPA_COMPLIANCE, value = "true"))
@DomainModel(annotatedClasses = { Employee.class, Department.class })
@SessionFactory()
public class FollowOnLockingTests {
	@Test
	public void testQueryLockingBaseline(SessionFactoryScope scope) {
		performQueryLocking( scope, false );
	}
	@Test
	public void testQueryLocking(SessionFactoryScope scope) {
		performQueryLocking( scope, true );
	}

	protected void performQueryLocking(SessionFactoryScope scope, boolean useFollowOnLocking) {
		scope.inTransaction( (session) -> {
			final QueryImplementor<Employee> query = session.createQuery(
					"select e from Employee e where e.salary > 10",
					Employee.class
			);
			query.setLockMode( LockModeType.PESSIMISTIC_READ );
			if ( useFollowOnLocking ) {
				query.setFollowOnLocking( true );
			}
			final List<Employee> employees = query.list();

			assertThat( employees ).hasSize( 1 );
			final LockModeType appliedLockMode = session.getLockMode( employees.get( 0 ) );
			assertThat( appliedLockMode ).isIn( LockModeType.PESSIMISTIC_READ, LockModeType.PESSIMISTIC_WRITE );

			try {
				// with the initial txn still active (locks still held), try to update the row from another txn
				scope.inTransaction( (session2) -> {
					final Employee june = session2.find( Employee.class, 3 );
					june.setSalary( 90000F );
				} );
				fail( "Locked entity update was allowed" );
			}
			catch (PessimisticLockException | LockTimeoutException expected) {
			}
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Department engineering = new Department( 1, "Engineering" );
			session.persist( engineering );

			session.persist( new Employee( 1, "John", 9F, engineering ) );
			session.persist( new Employee( 2, "Mary", 10F, engineering ) );
			session.persist( new Employee( 3, "June", 11F, engineering ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Employee" ).executeUpdate();
			session.createMutationQuery( "delete Department" ).executeUpdate();
		} );
	}
}
