/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.jpa;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.LockModeType;
import jakarta.persistence.Tuple;

@DomainModel(annotatedClasses = { Department.class })
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialect(value = SQLServerDialect.class)
@RequiresDialect(value = SybaseASEDialect.class)
public class AdvancedFollowOnLockingTest {

	@Test
	@JiraKey("HHH-17421")
	public void testNoFollowonLocking(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					final Department engineering = new Department( 1, "Engineering" );
					session.persist( engineering );
				}
		);

		scope.inTransaction(
				session -> {
					statementInspector.clear();

					final Query<Department> query = session.createQuery(
							"select distinct d from Department d",
							Department.class
					);
					query.setLockMode( LockModeType.PESSIMISTIC_WRITE );
					query.list();

					// The only statement should be the initial SELECT .. WITH (UPDLOCK, ..) ..
					// and without any follow-on locking.
					statementInspector.assertExecutedCount( 1 );
				}
		);
	}

	@Test
	@JiraKey("HHH-17421")
	public void testNoFollowonLockingOnGroupBy(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
								final Department engineering = new Department( 1, "Engineering" );
								session.persist( engineering );
							}
		);

		scope.inTransaction(
				session -> {
					statementInspector.clear();

					final Query<Tuple> query = session.createQuery(
							"select d, count(*) from Department d left join Department d2 on d.name = d2.name group by d",
							Tuple.class
					);
					query.setLockMode( LockModeType.PESSIMISTIC_WRITE );
					query.list();

					// The only statement should be the initial SELECT .. WITH (UPDLOCK, ..) ..
					// and without any follow-on locking.
					statementInspector.assertExecutedCount( 1 );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
