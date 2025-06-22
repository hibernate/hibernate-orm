/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.jpa;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.QueryTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = { Employee.class, Department.class })
@SessionFactory(useCollectingStatementInspector = true)
@SkipForDialect(dialectClass = HSQLDialect.class, reason = "Seems HSQLDB doesn't cancel the query if it waits for a lock?!")
@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Cockroach allows the concurrent access but cancels one or both transactions at the end")
@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 11, reason = "Timeouts don't work on Oracle 11 when using a driver other than ojdbc6, but we can't test with that driver")
@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase does not support timeout in statement level")
public class FollowOnLockingTest {

	@Test
	@Timeout(value = 2, unit = TimeUnit.MINUTES)
	public void testQueryLockingWithoutFollowOn(SessionFactoryScope scope) {
		testQueryLocking( scope, false );
	}
	@Test
	@Timeout(value = 2, unit = TimeUnit.MINUTES)
	public void testQueryLockingWithFollowOn(SessionFactoryScope scope) {
		testQueryLocking( scope, true );
	}

	public void testQueryLocking(SessionFactoryScope scope, boolean followOnLocking) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inSession( (s) -> {
			// After a transaction commit, the lock mode is set to NONE, which the TCK also does
			scope.inTransaction(
					s,
					session -> {
						final Department engineering = new Department( 1, "Engineering" );
						session.persist( engineering );

						session.persist( new Employee( 1, "John", 9F, engineering ) );
						session.persist( new Employee( 2, "Mary", 10F, engineering ) );
						session.persist( new Employee( 3, "June", 11F, engineering ) );
					}
			);

			scope.inTransaction(
					s,
					session -> {
						statementInspector.clear();

						final Query<Employee> query = session.createQuery(
								"select e from Employee e where e.salary > 10",
								Employee.class
						);
						if ( followOnLocking ) {
							query.setFollowOnLocking( true );
						}
						query.setLockMode( LockModeType.PESSIMISTIC_READ );
						final List<Employee> employees = query.list();

						assertThat( employees ).hasSize( 1 );
						final LockModeType appliedLockMode = session.getLockMode( employees.get( 0 ) );
						assertThat( appliedLockMode ).isIn(
								LockModeType.PESSIMISTIC_READ,
								LockModeType.PESSIMISTIC_WRITE
						);

						if ( followOnLocking ) {
							statementInspector.assertExecutedCount( 2 );
						}
						else {
							statementInspector.assertExecutedCount( 1 );
						}

						try {
							// with the initial txn still active (locks still held), try to update the row from another txn
							scope.inTransaction( (session2) -> {
								session2.createMutationQuery( "update Employee e set salary = 90000 where e.id = 3" )
										.setTimeout( 1 )
										.executeUpdate();
							} );
							fail( "Locked entity update was allowed" );
						}
						catch (PessimisticLockException | LockTimeoutException | QueryTimeoutException expected) {
						}
					}
			);
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
