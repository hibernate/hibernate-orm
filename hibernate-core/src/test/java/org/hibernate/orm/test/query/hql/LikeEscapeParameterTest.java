/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@DomainModel(
		annotatedClasses = LikeEscapeParameterTest.TestEntity.class
)
@SessionFactory
public class LikeEscapeParameterTest {

	@Test
	public void testSetCharPositionalParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select s from TestEntity s where s.name like ?1 escape ?2", TestEntity.class )
							.setParameter( 1, "%Foo" )
							.setParameter( 2, '\\' );
				}
		);
	}

	@Test
	public void testSetCharNamedParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery(
									"select s from TestEntity s where s.name like :likevalue escape :escapevalue",
									TestEntity.class
							)
							.setParameter( "likevalue", "%Foo" )
							.setParameter( "escapevalue", '\\' );
				}
		);
	}

	@Test
	public void testStringLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select s from TestEntity s where s.name like ?1 escape '\\'", TestEntity.class )
							.setParameter( 1, "%Foo" );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-15745")
	public void testStringLiteralInSubQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery(
									"select s from TestEntity s where s.name like ?1 escape '\\'" +
											" or s.id in (" +
											" select distinct t.id from TestEntity t where t.name like ?2 escape '\\'" +
											" )",
									TestEntity.class
							)
							.setParameter( 1, "%Foo" )
							.setParameter( 2, "Bar%" );
				}
		);
	}

	@Test
	public void testInvalidStringLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						session.createQuery(
								"select s from TestEntity s where s.name like ?1 escape 'AB'",
								TestEntity.class
						);
						Assertions.fail( "Expected a multi-character like predicate escape literal to fail" );
					}
					catch (IllegalArgumentException ex) {
						Assertions.assertTrue( ex.getCause() instanceof SemanticException );
						Assertions.assertTrue( ex.getCause().getMessage().contains( "single character" ) );
					}
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue
		private Integer id;

		private String name;
	}
}
