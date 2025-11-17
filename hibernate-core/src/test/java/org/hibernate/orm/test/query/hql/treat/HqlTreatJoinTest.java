/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.treat;

import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				HqlTreatJoinTest.TestEntity.class,
				HqlTreatJoinTest.JoinedEntity.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15839")
public class HqlTreatJoinTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					JoinedEntity joined = new JoinedEntity( 1, "joined" );
					TestEntity testEntity = new TestEntity( 2, "test", joined );

					session.persist( testEntity );
					session.persist( joined );

				}
		);

	}

	@Test
	public void testTreatQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query<TestEntity> query = session.createQuery(
							"select t from TestEntity t where treat(t.joined as JoinedEntity).id = ?1",
							TestEntity.class
					);
					query.setParameter( 1, 1 );
					List<TestEntity> result = query.list();
					assertThat( result.size() ).isEqualTo( 1 );
				}
		);
	}

	@MappedSuperclass
	public static abstract class AbstractEntity<T> {

		public AbstractEntity() {
		}

		public AbstractEntity(T joined) {
			this.joined = joined;
		}

		@OneToOne
		private T joined;

		public T getJoined() {
			return joined;
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity extends AbstractEntity<JoinedEntity> {
		@Id
		private long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(long id, String name, JoinedEntity joined) {
			super( joined );
			this.id = id;
			this.name = name;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "JoinedEntity")
	public static class JoinedEntity {

		@Id
		private long id;

		private String name;

		public JoinedEntity() {
		}

		public JoinedEntity(long id, String name) {
			this.id = id;
			this.name = name;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
