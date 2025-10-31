/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@Jpa(annotatedClasses = {TreatListJoinTest.TestEntity.class, TreatListJoinTest.EntityA.class, TreatListJoinTest.EntityB.class})
public class TreatListJoinTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			for ( int i = 0; i < 5; i++ ) {
				TestEntity e = new TestEntity();
				EntityA eA = new EntityA();
				eA.setParent( e );
				eA.valueA = "a_" + i;

				entityManager.persist( e );
			}
			for ( int i = 0; i < 5; i++ ) {
				TestEntity e = new TestEntity();

				EntityB eB = new EntityB();
				eB.valueB = "b_" + i;
				eB.setParent( e );

				entityManager.persist( e );
			}

		} );
	}

	@Test
	public void testTreatJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			final CriteriaQuery<Tuple> query = cb.createTupleQuery();
			final Root<TestEntity> testEntity = query.from( TestEntity.class );

			final List<Selection<?>> selections = new LinkedList<>();
			selections.add( testEntity.get( "id" ) );

			final ListJoin<TestEntity, AbstractEntity> entities = testEntity.joinList(
					"entities",
					JoinType.LEFT
			);
			entities.on( cb.equal( entities.get( "entityType" ), EntityA.class.getName() ) );

			final ListJoin<TestEntity, EntityA> joinEntityA = cb.treat(
					entities,
					EntityA.class
			);
			selections.add( joinEntityA.get( "id" ) );
			selections.add( joinEntityA.get( "valueA" ) );

			final ListJoin<TestEntity, AbstractEntity> entitiesB = testEntity.joinList(
					"entities",
					JoinType.LEFT
			);
			entitiesB.on( cb.equal( entitiesB.get( "entityType" ), EntityB.class.getName() ) );
			final ListJoin<TestEntity, EntityB> joinEntityB = cb.treat(
					entitiesB,
					EntityB.class
			);
			selections.add( joinEntityB.get( "id" ) );
			selections.add( joinEntityB.get( "valueB" ) );

			query.multiselect( selections );

			final List<Tuple> resultList = entityManager.createQuery( query ).getResultList();
			assertThat( resultList.size(), is( 10 ) );
		} );
	}

	@MappedSuperclass
	public static abstract class MyEntity {
		@Id
		@GeneratedValue
		private long id;
	}

	@Entity(name = "TestEntity")
	public static class TestEntity extends MyEntity {
		@OneToMany(mappedBy = "parent", cascade = {CascadeType.ALL})
		private List<AbstractEntity> entities = new ArrayList<>();
	}

	@Entity(name = "AbstractEntity")
	public static abstract class AbstractEntity extends MyEntity {
		String entityType = getClass().getName();

		@ManyToOne
		private TestEntity parent;

		public TestEntity getParent() {
			return parent;
		}

		public void setParent(TestEntity parent) {
			this.parent = parent;
			parent.entities.add( this );
		}
	}

	@Entity(name = "EntityA")
	public static class EntityA extends AbstractEntity {
		public String valueA;

		public EntityA() {
			super.entityType = getClass().getName();
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB extends AbstractEntity {
		public String valueB;

		public EntityB() {
			super.entityType = getClass().getName();
		}
	}
}
