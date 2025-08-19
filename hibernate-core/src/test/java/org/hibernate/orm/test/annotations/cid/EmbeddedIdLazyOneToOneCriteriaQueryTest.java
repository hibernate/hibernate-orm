/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Hibernate;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		EmbeddedIdLazyOneToOneCriteriaQueryTest.EntityA.class,
		EmbeddedIdLazyOneToOneCriteriaQueryTest.EntityB.class,
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19687")
@BytecodeEnhanced
public class EmbeddedIdLazyOneToOneCriteriaQueryTest {

	@Test
	public void query(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder builder = session.getCriteriaBuilder();
			final CriteriaQuery<EntityA> criteriaQuery = builder.createQuery( EntityA.class );
			final Root<EntityA> root = criteriaQuery.from( EntityA.class );
			criteriaQuery.where( root.get( "id" ).in( 1 ) );
			criteriaQuery.select( root );

			final List<EntityA> entities = session.createQuery( criteriaQuery ).getResultList();
			assertThat( entities ).hasSize( 1 );
			assertThat( Hibernate.isPropertyInitialized( entities.get( 0 ), "entityB" ) ).isFalse();
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = new EntityA( 1 );
			session.persist( entityA );
			final EntityB entityB = new EntityB( new EntityBId( entityA ) );
			session.persist( entityB );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.getSessionFactory().getSchemaManager().truncateMappedObjects() );
	}

	@Entity(name = "EntityA")
	static class EntityA {

		@Id
		private Integer id;

		@OneToOne(mappedBy = "id.entityA", fetch = FetchType.LAZY)
		private EntityB entityB;

		public EntityA() {
		}

		public EntityA(Integer id) {
			this.id = id;
		}

	}

	@Entity(name = "EntityB")
	static class EntityB {

		@EmbeddedId
		private EntityBId id;

		public EntityB() {
		}

		public EntityB(EntityBId id) {
			this.id = id;
		}

	}

	@Embeddable
	static class EntityBId {

		@OneToOne(fetch = FetchType.LAZY)
		private EntityA entityA;

		public EntityBId() {
		}

		public EntityBId(EntityA entityA) {
			this.entityA = entityA;
		}

	}

}
