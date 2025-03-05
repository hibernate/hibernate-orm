/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.sql.ast.SqlAstJoinType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@DomainModel(
		annotatedClasses = { HqlExplicitLeftJoinTest.EntityA.class, HqlExplicitLeftJoinTest.EntityB.class }
)
@SessionFactory(
		useCollectingStatementInspector = true
)
@JiraKey( value = "HHH-15342")
public class HqlExplicitLeftJoinTest {

	@Test
	public void testExplicitLeftJoin(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					session.createQuery(
									"Select a From EntityA a " +
											"Left Join a.entityB b " +
											"Where ( b.id IS NOT NULL )" )
							.getResultList();
					// The SQL should contain only the LEFT JOIN ( not inner join for the #@NotFound )
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );

					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 1 );
				}
		);
	}

	@Test
	public void testExplicitLeftJoin2(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					session.createQuery(
									"Select a From EntityA a " +
											"Left Join a.entityB b " +
											"Where ( b.name IS NOT NULL )" )
							.getResultList();
					// The SQL should contain only the LEFT JOIN ( not inner join for the #@NotFound )
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );

					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 1 );
				}
		);
	}

	@Test
	public void testImplicitJoin(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					session.createQuery(
									"Select a From EntityA a " +
											"Left Join a.entityB b " +
											"Join a.entityB " +
											"Where ( a.entityB.name IS NOT NULL )" )
							.getResultList();
					// The SQL should contain only the INNER JOIN ( The LEFT one will not be rendered due to optimizations )
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );

					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 0 );
				}
		);
	}

	@Test
	public void testImplicitJoin2(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					session.createQuery(
									"Select a From EntityA a " +
											"Join a.entityB " +
											"Where ( a.entityB.name IS NOT NULL )" )
							.getResultList();
					// The SQL should contain only one INNER JOIN
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );

					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 0 );
				}
		);
	}

	@Test
	public void testImplicitJoinWithNoExplicitJoins(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					session.createQuery(
									"Select a From EntityA a " +
											"Where ( a.entityB.name IS NOT NULL )" )
							.getResultList();
					// The SQL should contain only one INNER JOIN
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );

					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 0 );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		private EntityB entityB;

	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		private Integer id;

		private String name;

	}

}
