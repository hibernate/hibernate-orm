/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.treat;

import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				HqlTreatJoinFetchTest.TestEntity.class,
				HqlTreatJoinFetchTest.BaseEntity.class,
				HqlTreatJoinFetchTest.JoinedEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-17411")
public class HqlTreatJoinFetchTest {

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
	public void testTreatJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query<TestEntity> query = session.createQuery(
							"select t from TestEntity t join fetch treat(t.joined as JoinedEntity) j left join fetch j.testEntity e",
							TestEntity.class
					);
					List<TestEntity> result = query.list();
					assertThat( result.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testJoinFetchRootTreat(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query<BaseEntity> query = session.createQuery(
							"select t from BaseEntity t join fetch treat(t as JoinedEntity).testEntity j left join fetch j.joined2 e",
							BaseEntity.class
					);
					query.list();
				}
		);
	}

	@MappedSuperclass
	public static abstract class AbstractEntity {

		public AbstractEntity() {
		}

		public AbstractEntity(BaseEntity joined) {
			this.joined = joined;
		}

		@OneToOne
		private BaseEntity joined;

		public BaseEntity getJoined() {
			return joined;
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity extends AbstractEntity {
		@Id
		private long id;

		private String name;
		@ManyToOne(fetch = FetchType.LAZY)
		private JoinedEntity joined2;

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

	@Entity(name = "BaseEntity")
	public static abstract class BaseEntity {

		@Id
		private long id;

		private String name;

		public BaseEntity() {
		}

		public BaseEntity(long id, String name) {
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
	public static class JoinedEntity extends BaseEntity {
		@ManyToOne(fetch = FetchType.LAZY)
		private TestEntity testEntity;

		public JoinedEntity() {
		}

		public JoinedEntity(long id, String name) {
			super( id, name );
		}

		public TestEntity getTestEntity() {
			return testEntity;
		}
	}
}
