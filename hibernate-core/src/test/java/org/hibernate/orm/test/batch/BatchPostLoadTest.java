/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.annotations.BatchSize;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				BatchPostLoadTest.MyEntity1.class,
				BatchPostLoadTest.MyEntity2.class
		}
)
@JiraKey(value = "HHH-16106")
public class BatchPostLoadTest {

	private static final Long MY_ENTITY_1_ID = 1l;

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					MyEntity2 entity2 = new MyEntity2( 2l );
					entityManager.persist( entity2 );

					MyEntity1 entity1 = new MyEntity1( MY_ENTITY_1_ID, entity2 );
					entityManager.persist( entity1 );

					Long entity1Id = entity1.getId();
					assertEquals( entity1Id, entity1.getId() );
				}
		);
	}

	@Test
	public void testIt(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<MyEntity1> criteria = builder.createQuery( MyEntity1.class );
					Root<MyEntity1> root = criteria.from( MyEntity1.class );
					criteria.select( root );
					MyEntity1 entity1 = entityManager.createQuery( criteria ).getResultList().get( 0 );
					assertEquals( MY_ENTITY_1_ID, entity1.getId() );
					assertNotNull( entity1.getRef() );
				}
		);
	}

	@Entity
	public static class MyEntity1 {
		@Id
		private Long id;

		private Long version;

		@ManyToOne
		@JoinColumn(name = "ref")
		private MyEntity2 ref;

		public MyEntity1() {
		}

		public MyEntity1(Long id, MyEntity2 ref) {
			this.id = id;
			this.ref = ref;
		}

		public Long getId() {
			return id;
		}

		public Long getVersion() {
			return version;
		}

		public MyEntity2 getRef() {
			return ref;
		}

		@PostLoad
		public void test() {
			assertNotNull( getRef() );
		}
	}

	@Entity
	@BatchSize(size = 100)
	public static class MyEntity2 {
		@Id
		private Long id;

		private String name;

		public MyEntity2() {
		}

		public MyEntity2(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

	}

}
