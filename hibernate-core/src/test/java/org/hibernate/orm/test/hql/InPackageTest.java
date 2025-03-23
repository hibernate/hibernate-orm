/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import in.from.Any;
import in.from.TestEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {Any.class, TestEntity.class}
)
@SessionFactory
@JiraKey("HHH-13943")
public class InPackageTest {

	@BeforeAll
	public static void init(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Any( "a" ) );
					session.persist( new TestEntity( "t", null ) );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Long> ids = session.createQuery( "select a.id from in.from.Any a ", Long.class ).list();
					assertThat( ids.size() ).isEqualTo( 1 );
				}
		);
		scope.inTransaction(
				session -> {
					List<Long> ids = session.createQuery( "select a.id from `in.from.Any` a ", Long.class ).list();
					assertThat( ids.size() ).isEqualTo( 1 );
				}
		);
		scope.inTransaction(
				session -> {
					List<Long> ids = session.createQuery(
									"select t.id from in.from.TestEntity t join in.from.Any a on a.id=t.id", Long.class )
							.list();
					assertThat( ids.size() ).isEqualTo( 1 );

				}
		);
		scope.inTransaction(
				session -> {
					List<Long> ids = session.createQuery(
									"select t.id from `in.from.TestEntity` t join `in.from.Any` a on a.id=t.id", Long.class )
							.list();
					assertThat( ids.size() ).isEqualTo( 1 );

				}
		);

		scope.inTransaction(
				session -> {
					List<Long> ids = session.createQuery(
									"select t.id from in.from.TestEntity t join `in.from.Any` a on a.id=t.id", Long.class )
							.list();
					assertThat( ids.size() ).isEqualTo( 1 );

				}
		);
	}
}
