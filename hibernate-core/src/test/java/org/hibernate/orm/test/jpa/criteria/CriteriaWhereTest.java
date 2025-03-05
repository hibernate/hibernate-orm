/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = CriteriaWhereTest.TestEntity.class
)
@JiraKey(value = "HHH-15716")
public class CriteriaWhereTest {


	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager ->
						entityManager.persist( new TestEntity( 1l, "And" ) )
		);
	}

	@Test
	public void testSelect(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TestEntity entity = new TestEntity( 1l, "And" );

					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<TestEntity> query = cb.createQuery( TestEntity.class );
					Root<TestEntity> root = query.from( TestEntity.class );

					CriteriaQuery<TestEntity> criteriaQuery = query.select( root ).where( cb.equal( root, entity ) );
					List<TestEntity> entities = entityManager.createQuery( criteriaQuery ).getResultList();
					assertThat( entities.size() ).isEqualTo( 1 );
					assertThat( entities.get( 0 ).getName() ).isEqualTo( "And" );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
